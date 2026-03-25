package org.calypsonet.keyple.demo.control.domain.model

data class Location(val id: Int, val name: String) {
    override fun toString(): String {
        return "$id - $name"
    }
}
