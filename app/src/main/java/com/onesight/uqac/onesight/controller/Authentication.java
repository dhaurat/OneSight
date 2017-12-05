package com.onesight.uqac.onesight.controller;

import com.google.firebase.auth.FirebaseUser;


public interface Authentication
{
    void checkUserLogIn(FirebaseUser user);
}
