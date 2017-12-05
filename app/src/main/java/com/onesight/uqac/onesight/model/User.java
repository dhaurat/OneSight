package com.onesight.uqac.onesight.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Damien on 02/11/2017.
 * User class
 */

public class User {

    //private static AtomicInteger next_id = new AtomicInteger(1);

    //private int id;
    public String surname;
    public String name;
    public String birthDate;
    public Sex sex;
    public Sex orientation;
    public String photoUrl;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    // Used when registering a new user into database
    public User(String surname, String name, String birthDate, Sex sex, Sex orientation) {
        this.surname = surname;
        this.name = name;
        this.birthDate = birthDate;
        this.sex = sex;
        this.orientation = orientation;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("surname", surname);
        result.put("name", name);
        result.put("birthDate", birthDate);
        result.put("sex", sex);
        result.put("orientation", orientation);

        return result;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public Sex getSex()
    {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public Sex getSearchedSex() {
        return orientation;
    }

    public void setSearchedSex(Sex searched_sex) {
        this.orientation = searched_sex;
    }

    public boolean isMale(){
        return this.sex == Sex.MALE;
    }

    public void setPhoto(String photoUrl) { this.photoUrl = photoUrl; }

}
