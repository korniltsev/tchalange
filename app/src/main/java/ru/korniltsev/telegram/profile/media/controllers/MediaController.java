package ru.korniltsev.telegram.profile.media.controllers;

import java.util.Set;

public abstract class MediaController {

    public abstract Set<Integer> getSelectedMessagesIds();
    public abstract void drop();

    public abstract void dropSelection() ;

    public abstract void messagesDeleted(int[] msgIds) ;
}


