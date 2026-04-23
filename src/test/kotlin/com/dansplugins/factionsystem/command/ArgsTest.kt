package com.dansplugins.factionsystem.command

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test

class ArgsTest {
    // dropFirst tests

    @Test
    fun `dropFirst on empty array returns empty array`() {
        assertArrayEquals(emptyArray<String>(), emptyArray<String>().dropFirst())
    }

    @Test
    fun `dropFirst on single-element array returns empty array`() {
        assertArrayEquals(emptyArray<String>(), arrayOf("a").dropFirst())
    }

    @Test
    fun `dropFirst removes first element`() {
        assertArrayEquals(arrayOf("b", "c"), arrayOf("a", "b", "c").dropFirst())
    }

    // unquote tests

    @Test
    fun `unquote with no quotes returns original args`() {
        assertArrayEquals(arrayOf("hello", "world"), arrayOf("hello", "world").unquote())
    }

    @Test
    fun `unquote merges quoted tokens spanning multiple args`() {
        // "hello world" foo  ->  hello world, foo
        assertArrayEquals(
            arrayOf("hello world", "foo"),
            arrayOf("\"hello", "world\"", "foo").unquote()
        )
    }

    @Test
    fun `unquote handles single-arg quoted string`() {
        // "hello" -> hello
        assertArrayEquals(arrayOf("hello"), arrayOf("\"hello\"").unquote())
    }

    @Test
    fun `unquote handles empty quoted string`() {
        // "" -> (empty string)
        assertArrayEquals(arrayOf(""), arrayOf("\"\"").unquote())
    }

    @Test
    fun `unquote with unbalanced opening quote treats remaining args as one token`() {
        // "hello world  (no closing quote) -> hello world
        assertArrayEquals(arrayOf("hello world"), arrayOf("\"hello", "world").unquote())
    }

    @Test
    fun `unquote arg without quotes is passed through unchanged`() {
        assertArrayEquals(arrayOf("hello"), arrayOf("hello").unquote())
    }

    @Test
    fun `unquote arg with trailing quote but no opening quote is passed through unchanged`() {
        // hello"  -> hello"  (no opening quote was seen, so trailing quote is not stripped)
        assertArrayEquals(arrayOf("hello\""), arrayOf("hello\"").unquote())
    }

    @Test
    fun `unquote handles multiple quoted segments`() {
        // "foo bar" "baz qux" -> foo bar, baz qux
        assertArrayEquals(
            arrayOf("foo bar", "baz qux"),
            arrayOf("\"foo", "bar\"", "\"baz", "qux\"").unquote()
        )
    }
}
