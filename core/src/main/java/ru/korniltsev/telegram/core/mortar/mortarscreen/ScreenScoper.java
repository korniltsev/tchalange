package ru.korniltsev.telegram.core.mortar.mortarscreen;

import android.content.Context;
import android.content.res.Resources;
import mortar.MortarScope;
import mortar.ViewPresenter;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.core.mortar.ViewPresenterHolder;

import static java.lang.String.format;


public class ScreenScoper {
//  private static final ModuleFactory NO_FACTORY = new ModuleFactory() {
//    @Override protected Object createDaggerModule(Resources resources, Object screen) {
//      throw new UnsupportedOperationException();
//    }
//  };

//  private final Map<Class, ModuleFactory> moduleFactoryCache = new LinkedHashMap<>();

  public MortarScope getScreenScope(Context context, String name, Object screen) {
    MortarScope parentScope = MortarScope.getScope(context);
    return getScreenScope(context, parentScope, name, screen);
  }


  public MortarScope getScreenScope(Context ctx, MortarScope parentScope, final String name,
      final Object screen) {
    ModuleFactory moduleFactory = getModuleFactory(screen);
    Object childModule = moduleFactory.createDaggerModule(ctx, screen);

    ViewPresenterHolder viewPresenterHolder = null;
    if (screen instanceof ViewPresenterHolder.Factory) {
      final ViewPresenterHolder.Factory factory = (ViewPresenterHolder.Factory) screen;
      final ViewPresenter viewPresenter = factory.create(ctx);
      viewPresenterHolder = new ViewPresenterHolder(viewPresenter);
    }

    MortarScope childScope = parentScope.findChild(name);
    if (childScope == null) {
      final MortarScope.Builder builder = parentScope.buildChild()
              .withService(ObjectGraphService.SERVICE_NAME,
                      ObjectGraphService.create(parentScope, childModule));
      if (viewPresenterHolder != null) {
        builder.withService(ViewPresenterHolder.SERVICE_NAME, viewPresenterHolder);
      }
      childScope = builder
          .build(name);
    }

    return childScope;
  }

  private ModuleFactory getModuleFactory(Object screen) {
    if (screen instanceof ModuleFactory2){
      return new DelegateModuleFactory((ModuleFactory2) screen);
    } else {
      throw new RuntimeException("screen should implement ModuleFactory2");
    }
//    Class<?> screenType = screen.getClass();
//    ModuleFactory moduleFactory = moduleFactoryCache.get(screenType);
//
//    if (moduleFactory != null) return moduleFactory;
//
//    WithModule withModule = screenType.getAnnotation(WithModule.class);
//    if (withModule != null) {
//      Class<?> moduleClass = withModule.value();
//
//      Constructor<?>[] constructors = moduleClass.getDeclaredConstructors();
//
//      if (constructors.length != 1) {
//        throw new IllegalArgumentException(
//            format("Module %s for screen %s should have exactly one public constructor",
//                moduleClass.getName(), screen));
//      }
//
//      Constructor constructor = constructors[0];
//
//      Class[] parameters = constructor.getParameterTypes();
//
//      if (parameters.length > 1) {
//        throw new IllegalArgumentException(
//            format("Module %s for screen %s should have 0 or 1 parameter", moduleClass.getName(),
//                screen));
//      }
//
//      Class screenParameter;
//      if (parameters.length == 1) {
//        screenParameter = parameters[0];
//        if (!screenParameter.isInstance(screen)) {
//          throw new IllegalArgumentException(format("Module %s for screen %s should have a "
//                  + "constructor parameter that is a super class of %s", moduleClass.getName(),
//              screen, screen.getClass().getName()));
//        }
//      } else {
//        screenParameter = null;
//      }
//
//      try {
//        if (screenParameter == null) {
//          moduleFactory = new NoArgsFactory(constructor);
//        } else {
//          moduleFactory = new SingleArgFactory(constructor);
//        }
//      } catch (Exception e) {
//        throw new RuntimeException(
//            format("Failed to instantiate module %s for screen %s", moduleClass.getName(), screen),
//            e);
//      }
//    }
//
//    if (moduleFactory == null) {
//      WithModuleFactory withModuleFactory = screenType.getAnnotation(WithModuleFactory.class);
//      if (withModuleFactory != null) {
//        Class<? extends ModuleFactory> mfClass = withModuleFactory.value();
//
//        try {
//          moduleFactory = mfClass.newInstance();
//        } catch (Exception e) {
//          throw new RuntimeException(format("Failed to instantiate module factory %s for screen %s",
//              withModuleFactory.value().getName(), screen), e);
//        }
//      }
//    }
//
//    if (moduleFactory == null) moduleFactory = NO_FACTORY;
//
//    moduleFactoryCache.put(screenType, moduleFactory);
//
//    return moduleFactory;
  }

//  private static class NoArgsFactory extends ModuleFactory<Object> {
//    final Constructor moduleConstructor;
//
//    private NoArgsFactory(Constructor moduleConstructor) {
//      this.moduleConstructor = moduleConstructor;
//    }
//
//    @Override protected Object createDaggerModule(Resources resources, Object ignored) {
//      try {
//        return moduleConstructor.newInstance();
//      } catch (InstantiationException e) {
//        throw new RuntimeException(e);
//      } catch (IllegalAccessException e) {
//        throw new RuntimeException(e);
//      } catch (InvocationTargetException e) {
//        throw new RuntimeException(e);
//      }
//    }
//  }
//
//  private static class SingleArgFactory extends ModuleFactory {
//    final Constructor moduleConstructor;
//
//    public SingleArgFactory(Constructor moduleConstructor) {
//      this.moduleConstructor = moduleConstructor;
//    }
//
//    @Override protected Object createDaggerModule(Resources resources, Object screen) {
//      try {
//        return moduleConstructor.newInstance(screen);
//      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
//        throw new RuntimeException(e);
//      }
//    }
//  }

  private static class DelegateModuleFactory extends ModuleFactory {
    final ModuleFactory2 delegate;

    private DelegateModuleFactory(ModuleFactory2 delegate) {
      this.delegate = delegate;
    }

    @Override
    protected Object createDaggerModule(Context resources, Object screen) {
      return delegate.createDaggerModule();
    }
  }
}
