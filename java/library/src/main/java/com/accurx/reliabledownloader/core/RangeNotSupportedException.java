package com.accurx.reliabledownloader.core;

public class RangeNotSupportedException extends Exception {
    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_MESSAGE = "Range not supported";

    public RangeNotSupportedException() {}
}
