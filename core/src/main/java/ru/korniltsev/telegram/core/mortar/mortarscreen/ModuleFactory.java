package ru.korniltsev.telegram.core.mortar.mortarscreen;

import android.content.Context;
import android.content.res.Resources;

/** @see WithModuleFactory */
public abstract class ModuleFactory<T> {
  protected abstract Object createDaggerModule(Context resources, T screen);
}
