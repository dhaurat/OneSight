package com.onesight.uqac.onesight.controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class InputValidationHelper
{
    private static final String NAME_PATTERN = "[A-Za-zàéèêÀÉÈÊ'\\-]+";
    private static final int MIN_LENGTH = 8;

    static boolean isValidEmail(CharSequence target)
    {
        return (android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    //TODO: strengthen password constraints
    static boolean isValidPassword(CharSequence target)
    {
        return target.length() >= MIN_LENGTH;
    }

    static boolean isValidName(CharSequence target)
    {
        Pattern pattern = Pattern.compile(NAME_PATTERN);
        Matcher matcher = pattern.matcher(target);

        return (matcher.matches());
    }
}
