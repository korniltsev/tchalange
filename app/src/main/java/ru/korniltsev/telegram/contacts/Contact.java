package ru.korniltsev.telegram.contacts;

import org.drinkless.td.libcore.telegram.TdApi;

public class Contact {
    public final TdApi.User user;
    public final String uiName;
    public final String section;
    public final String uiStatus;

    public Contact(TdApi.User user, String uiName, String uiStatus) {
        this.user = user;
        this.uiName = uiName;
        this.uiStatus = uiStatus;
        if (this.uiName.isEmpty()) {
            section = "";
        } else {
            section = this.uiName.substring(0, 1);
        }
    }


}
