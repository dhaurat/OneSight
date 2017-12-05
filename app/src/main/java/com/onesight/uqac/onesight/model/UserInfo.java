package com.onesight.uqac.onesight.model;

public enum UserInfo {

    SHARED_PREFERENCES_ID ("onesight_prefs"),
    USER_NAME ("name"),
    USER_SURNAME ("surname"),
    USER_SEX ("sex"),
    USER_BIRTHDATE ("birthDate"),
    USER_ORIENTATION ("orientation"),
    USER_PHOTO ("photoURI"),
    ACTIVATED ("activated"),
    MATCH_ID ("match_id");

    private String info = "";

    UserInfo(String info)
    {
        this.info = info;
    }

    public String getInfo()
    {
        return info;
    }
}
