#!/usr/bin/env python3
"""Checks the log messages of all the components against the convention described in README.md ("Log Messages").

Checked rules:
- the message is an English sentence starting with a capital letter (or with a variable message), without final
  punctuation, without class name prefix ("SomeClass: ..."), its context being at its end between square brackets;
- the keys of the context are in camelCase;
- a warning is logged without stack trace (its cause is given in the `cause` key);
- a catch block logs the exception, throws an exception, or explains in a comment why it does not log (e.g. refusal
  already logged where it is detected).

Usage: check-log-messages.py (from the root of the repository)
"""
import os
import re
import sys

SOURCE_ROOT = "src"
EXTENSIONS = (".kt", ".java", ".cs")
EXCLUDED_DIRECTORIES = {"build", "node_modules", "bin", "obj", ".gradle", "test", "androidTest"}

# Log calls of SLF4J (logger.info), the Logger port (logger.i), Timber, Napier and Serilog (_logger.Information)
LOG_CALL = re.compile(
    r"\b(?:logger|_logger|Log|Timber|Napier)\."
    r"(v|d|i|w|e|trace|debug|info|warn|error|Verbose|Debug|Information|Warning|Error|Fatal)\(")
WARNING_LEVELS = {"w", "warn", "Warning"}
# String constants used as messages (Java)
CONSTANT = re.compile(r'static\s+final\s+String\s+([A-Z][A-Z0-9_]*)\s*=\s*("(?:[^"\\]|\\.)*")')
CATCH = re.compile(r"\bcatch\s*\(([^)]*)\)\s*\{")
HANDLED = re.compile(LOG_CALL.pattern + r"|\bthrow\b|//")


def source_files():
    for directory, directories, files in os.walk(SOURCE_ROOT):
        directories[:] = [d for d in directories if d not in EXCLUDED_DIRECTORIES]
        for name in files:
            if name.endswith(EXTENSIONS):
                yield os.path.join(directory, name)


def skip_string(text, i):
    """Returns the index following the string literal starting at i (Kotlin templates and C# interpolations included)."""
    if text[i] == "$" or text[i] == "@":
        i += 1
    i += 1
    while text[i] != '"':
        if text[i] == "\\":
            i += 1
        elif text.startswith("${", i):
            # Kotlin template: skips the expression, which may contain strings
            depth, i = 0, i + 1
            while True:
                if text[i] == '"':
                    i = skip_string(text, i) - 1
                elif text[i] == "{":
                    depth += 1
                elif text[i] == "}":
                    depth -= 1
                    if depth == 0:
                        break
                i += 1
        i += 1
    return i + 1


def call_arguments(text, start):
    """Returns the text of the arguments of the call whose opening parenthesis is at start, and the end index."""
    i, depth = start, 0
    while True:
        c = text[i]
        if c == '"' or (c in "$@" and text[i + 1] == '"'):
            i = skip_string(text, i)
            continue
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return text[start + 1:i], i + 1
        i += 1


def message_text(arguments, constants):
    """Returns the message of the log call (first string literal, joined to the following ones by '+'), or None."""
    arguments = re.sub(r'\btag\s*=\s*"[^"]*"\s*,?', "", arguments)
    for name, value in constants.items():
        arguments = re.sub(r"\b" + name + r"\b", value, arguments)
    match = re.search(r'[$@]?"', arguments)
    if not match:
        return None
    parts, i = [], match.start()
    while True:
        end = skip_string(arguments, i)
        literal = arguments[i:end]
        parts.append(literal[literal.index('"') + 1:-1])
        following = re.match(r'\s*\+\s*([$@]?")', arguments[end:])
        if not following:
            return "".join(parts)
        i = end + following.start(1)


def message_violations(message):
    violations = []
    # The variable values in the context are replaced, to check the text only
    text = re.sub(r"\$\{[^}]*\}|\$\w+|\{[^}]*\}", "X", message)
    if not re.match(r"^[A-Z]", text) and not message.startswith(("$", "{")):
        violations.append("the message must start with a capital letter")
    if re.search(r"[.!:,;]\s*$", text):
        violations.append("the message must not end with a punctuation mark")
    if re.match(r"^[A-Z][a-z0-9]+[A-Z]\w*:", text):
        violations.append("the message must not start with a class name")
    context = re.search(r"\[(.*)\]\s*$", message)
    if "[" in text and not context:
        violations.append("the context must be at the end of the message, between square brackets")
    if context:
        for key in re.findall(r"(?:^|,\s*)([^=,\s]+)=", context.group(1)):
            if not re.match(r"^[a-z][A-Za-z0-9]*$", key):
                violations.append("the key '%s' of the context must be in camelCase" % key)
    return violations


def main():
    violations = 0
    log_calls = 0
    catch_blocks = 0

    def report(path, text, index, message):
        nonlocal violations
        line = text.count("\n", 0, index) + 1
        print("::error file=%s,line=%d::%s" % (path.replace(os.sep, "/"), line, message))
        violations += 1

    for path in source_files():
        text = open(path, encoding="utf-8-sig").read()
        constants = dict(CONSTANT.findall(text))
        for call in LOG_CALL.finditer(text):
            # Calls in comments are ignored
            line_start = text.rfind("\n", 0, call.start()) + 1
            if text[line_start:call.start()].lstrip().startswith(("//", "*")):
                continue
            log_calls += 1
            arguments, _ = call_arguments(text, call.end() - 1)
            message = message_text(arguments, constants)
            if message is not None:
                for violation in message_violations(message):
                    report(path, text, call.start(), '%s: "%s"' % (violation, message))
            if call.group(1) in WARNING_LEVELS:
                first, last = arguments.split(",")[0].strip(), arguments.split(",")[-1].strip()
                if re.match(r"^(e|ex|throwable)$", last) or re.match(r"^(e|ex)$", first):
                    report(path, text, call.start(), "a warning must be logged without stack trace (cause in the 'cause' key)")
        for catch in CATCH.finditer(text):
            catch_blocks += 1
            i, depth = catch.end(), 1
            while depth:
                if text[i] == '"':
                    i = skip_string(text, i)
                    continue
                depth += {"{": 1, "}": -1}.get(text[i], 0)
                i += 1
            if not HANDLED.search(text[catch.end():i - 1]):
                report(path, text, catch.start(),
                       "the catch block must log the exception, throw an exception, or explain in a comment why it "
                       "does not log it")

    if violations:
        print("%d log message violation(s) found (see \"Log Messages\" in README.md)" % violations)
        sys.exit(1)
    print("Log messages checked: %d log calls and %d catch blocks, no violation" % (log_calls, catch_blocks))


if __name__ == "__main__":
    main()
