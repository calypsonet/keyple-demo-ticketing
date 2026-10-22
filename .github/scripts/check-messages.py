#!/usr/bin/env python3
"""Checks the log and exception messages of all the components against the conventions described in README.md ("Log
messages" and "Exception messages").

Checked rules:
- a message is an English sentence starting with a capital letter (or with a variable message), without final
  punctuation, without class name prefix ("SomeClass: ..."), its context being at its end between square brackets;
- the keys of the context are in camelCase;
- a warning is logged without stack trace (its cause is given in the `cause` key);
- a catch block logs the exception, throws an exception, or explains in a comment why it does not log (for example,
  rejection already logged where it is detected);
- an exception is of a specific type (not `Exception` nor `RuntimeException`), and its cause is chained to it instead of
  being copied in its message (a cause which cannot be chained is given in the `cause` key).

The Kotlin, Java, and C# sources are checked, and the JavaScript sources of the dashboard (console messages and errors,
written between double quotes).

Usage: check-messages.py (from the root of the repository)
"""
import os
import re
import sys

SOURCE_ROOT = "src"
EXTENSIONS = (".kt", ".java", ".cs", ".js", ".jsx")
EXCLUDED_DIRECTORIES = {"build", "dist", "node_modules", "bin", "obj", ".gradle", "test", "androidTest"}

# Log calls of SLF4J (logger.info), the Logger port (logger.i), Timber, Napier, Serilog (_logger.Information), and of
# the browser console (dashboard, console.warn)
LOG_CALL = re.compile(
    r"\b(?:logger|_logger|Log|Timber|Napier|console)\."
    r"(v|d|i|w|e|trace|debug|info|log|warn|error|Verbose|Debug|Information|Warning|Error|Fatal)\(")
WARNING_LEVELS = {"w", "warn", "Warning"}
# String constants used as messages (Java)
CONSTANT = re.compile(r'static\s+final\s+String\s+([A-Z][A-Z0-9_]*)\s*=\s*("(?:[^"\\]|\\.)*")')
CATCH = re.compile(r"\bcatch\s*\(([^)]*)\)\s*\{")
HANDLED = re.compile(LOG_CALL.pattern + r"|\bthrow\b|//")

# Exceptions thrown (Kotlin, Java, C#), Kotlin preconditions and messages of the exception classes (Java)
THROW = re.compile(r"\bthrow\s+(?:new\s+)?([A-Za-z_]\w*)\s*\(")
PRECONDITION = re.compile(r"(?<![.\w])(error|check|checkNotNull|require|requireNotNull)\s*\(")
SUPER = re.compile(r"\bsuper\s*\(")
EXCEPTION_CLASS = re.compile(r"\bclass\s+\w+\s+extends\s+\w*(?:Exception|Error)\b")
GENERIC_EXCEPTIONS = {"Exception", "RuntimeException", "Throwable", "Error"}
# Exceptions whose first argument is the name of the parameter (C#)
PARAMETER_FIRST = {"ArgumentNullException", "ArgumentOutOfRangeException"}
COPIED_CAUSE = re.compile(r"\b(?:e|ex|cause|throwable)\s*\.\s*(?:message|Message|localizedMessage|getMessage\(\))")


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


def is_string_start(text, i):
    return text[i] == '"' or (text[i] in "$@" and i + 1 < len(text) and text[i + 1] == '"')


def call_arguments(text, start):
    """Returns the text of the arguments of the call whose opening parenthesis is at start, and the end index."""
    i, depth = start, 0
    while True:
        if is_string_start(text, i):
            i = skip_string(text, i)
            continue
        c = text[i]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return text[start + 1:i], i + 1
        i += 1


def split_top_level(text, separator):
    """Splits the text on the separators which are neither in a string nor between brackets."""
    parts, depth, start, i = [], 0, 0, 0
    while i < len(text):
        if is_string_start(text, i):
            i = skip_string(text, i)
            continue
        c = text[i]
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == separator and depth == 0:
            parts.append(text[start:i])
            start = i + 1
        i += 1
    parts.append(text[start:])
    return parts


def message_text(arguments, constants):
    """Returns the message of the call: its first argument containing a string literal, the literals being joined to the
    variable parts (written {X}), or None if no argument contains a string literal."""
    arguments = re.sub(r'\btag\s*=\s*"[^"]*"\s*,?', "", arguments)
    for name, value in constants.items():
        arguments = re.sub(r"\b" + name + r"\b", value, arguments)
    for argument in split_top_level(arguments, ","):
        if not re.search(r'[$@]?"', argument):
            continue
        parts = []
        for operand in split_top_level(argument, "+"):
            operand = operand.strip()
            if operand and is_string_start(operand, 0) and skip_string(operand, 0) == len(operand):
                parts.append(operand[operand.index('"') + 1:-1])
            else:
                parts.append("{X}")
        return "".join(parts)
    return None


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
    # Out of the context, only a whole variable message is allowed (for example, "$failureLog [statusCode=...]")
    head = re.sub(r"^(\$\{[^}]*\}|\$\w+|\{[^}]*\})", "", message[:context.start()] if context else message)
    if re.search(r"\$\{[^}]*\}|\$\w+|\{[^}]*\}", head):
        violations.append("the variable values must be given in the context, between square brackets")
    if context:
        for key in re.findall(r"(?:^|,\s*)([^=,\s]+)=", context.group(1)):
            if not re.match(r"^[a-z][A-Za-z0-9]*$", key):
                violations.append("the key '%s' of the context must be in camelCase" % key)
    return violations


def in_comment(text, index):
    line_start = text.rfind("\n", 0, index) + 1
    return text[line_start:index].lstrip().startswith(("//", "*"))


def exception_messages(path, text):
    """Returns the exceptions of the file: (index, type or None, arguments giving the message)."""
    found = []
    for throw in THROW.finditer(text):
        if not in_comment(text, throw.start()):
            arguments, _ = call_arguments(text, throw.end() - 1)
            if throw.group(1) in PARAMETER_FIRST:
                arguments = arguments.split(",", 1)[-1]
            found.append((throw.start(), throw.group(1), arguments))
    if path.endswith(".kt"):
        for precondition in PRECONDITION.finditer(text):
            if in_comment(text, precondition.start()):
                continue
            arguments, end = call_arguments(text, precondition.end() - 1)
            if precondition.group(1) != "error":
                # message given by the lambda following the call, if any
                lazy_message = re.match(r"\s*\{\s*", text[end:])
                if not lazy_message or not is_string_start(text, end + lazy_message.end()):
                    continue
                start = end + lazy_message.end()
                arguments = text[start:skip_string(text, start)]
            found.append((precondition.start(), None, arguments))
    if EXCEPTION_CLASS.search(text):
        for call in SUPER.finditer(text):
            arguments, _ = call_arguments(text, call.end() - 1)
            found.append((call.start(), None, arguments))
    return found


def main():
    violations = 0
    log_calls = 0
    exceptions = 0
    catch_blocks = 0

    def report(path, text, index, message):
        nonlocal violations
        line = text.count("\n", 0, index) + 1
        print("::error file=%s,line=%d::%s" % (path.replace(os.sep, "/"), line, message))
        violations += 1

    for path in source_files():
        text = open(path, encoding="utf-8-sig").read()
        constants = dict(CONSTANT.findall(text))
        # Log messages
        for call in LOG_CALL.finditer(text):
            if in_comment(text, call.start()):
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
        # Exception messages
        for index, exception_type, arguments in exception_messages(path, text):
            exceptions += 1
            # Error is the exception type of JavaScript
            if exception_type in GENERIC_EXCEPTIONS and not (exception_type == "Error" and path.endswith((".js", ".jsx"))):
                report(path, text, index, "a specific exception type must be thrown instead of %s" % exception_type)
            message = message_text(arguments, constants)
            if message is not None:
                for violation in message_violations(message):
                    report(path, text, index, '%s: "%s"' % (violation, message))
            if COPIED_CAUSE.search(arguments) and (message is None or "cause=" not in message):
                report(path, text, index, "the cause must be chained to the exception, or its message given in the 'cause' key")
        # Catch blocks
        for catch in CATCH.finditer(text):
            catch_blocks += 1
            i, depth = catch.end(), 1
            while depth:
                if is_string_start(text, i):
                    i = skip_string(text, i)
                    continue
                depth += {"{": 1, "}": -1}.get(text[i], 0)
                i += 1
            if not HANDLED.search(text[catch.end():i - 1]):
                report(path, text, catch.start(),
                       "the catch block must log the exception, throw an exception, or explain in a comment why it "
                       "does not log it")

    if violations:
        print("%d message violation(s) found (see \"Log messages\" and \"Exception messages\" in README.md)" % violations)
        sys.exit(1)
    print("Messages checked: %d log calls, %d exceptions and %d catch blocks, no violation"
          % (log_calls, exceptions, catch_blocks))


if __name__ == "__main__":
    main()
