package com.fasterxml.jackson.databind.exc;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.deser.UnresolvedForwardReference;

public class UnresolvedForwardReferenceTest extends BaseMapTest
{
    public void testWithAndWithoutStackTraces() throws Exception
    {
        try (JsonParser p = sharedMapper().createParser("{}")) {
            UnresolvedForwardReference e = new UnresolvedForwardReference(p, "test");
            StackTraceElement[] stack = e.getStackTrace();
            assertEquals(0, stack.length);

            e = e.withStackTrace();
            stack = e.getStackTrace();
            if (stack.length < 1) {
                fail("Should have filled in stack traces, only got: "+stack.length);
            }
        }
    }
}
