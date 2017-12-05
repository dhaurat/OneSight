package com.onesight.uqac.onesight.model;

/**
 * Created by Damien on 02/11/2017.
 * enum to defined possible kinds of sexuality
 */

public enum Sex {
    MALE(1),
    FEMALE(2),
    ALL(3);

    private final int id;

    Sex(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getString()
    {
        switch (id)
        {
            case 1:
                return "MALE";
            case 2:
                return "FEMALE";
            case 3:
                return "ALL";
            default:
                return "";
        }
    }
}
