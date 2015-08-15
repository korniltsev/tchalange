package ru.korniltsev.telegram.core.rx;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.drinkless.td.libcore.telegram.TdApi;
import org.json.JSONException;
import org.json.JSONObject;

public class FastUserSerialization {

    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String ID = "id";
    public static final String PHONE_NUMBER = "phoneNumber";


//    public ProfilePhoto profilePhoto;
//    public LinkState myLink;
//    public LinkState foreignLink;
//    public UserType type;
    @NonNull public static String serialize(@NonNull TdApi.User user){
        try {
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put(FIRST_NAME, user.firstName);
            jsonObject.put(LAST_NAME, user.lastName);
            jsonObject.put(ID, user.id);
            jsonObject.put(PHONE_NUMBER, user.phoneNumber);
            return jsonObject.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable public static TdApi.User deserialize(@NonNull String s) {
        try {
            final JSONObject json = new JSONObject(s);
            final String firstName = json.getString(FIRST_NAME);
            final String lastName = json.getString(LAST_NAME);
            final String phone = json.getString(PHONE_NUMBER);
            final int myId = json.getInt(ID);
            final TdApi.User result = new TdApi.User();
            result.firstName = firstName;
            result.lastName = lastName;
            result.phoneNumber = phone;
            result.id = myId;
            result.status = new TdApi.UserStatusOnline();

            final TdApi.File empty = new TdApi.File(TdApi.File.NO_FILE_ID, "", 0, "");
            result.profilePhoto = new TdApi.ProfilePhoto(TdApi.File.NO_FILE_ID, empty, empty);
            return result;
        } catch (JSONException e) {
            return null;
        }
    }
}
