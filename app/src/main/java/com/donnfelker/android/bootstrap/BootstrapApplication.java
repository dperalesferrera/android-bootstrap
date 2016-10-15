

package com.donnfelker.android.bootstrap;

import android.app.Application;
import com.amazonaws.mobile.AWSMobileClient;

/**
 * Android Bootstrap application
 */
public abstract class BootstrapApplication extends Application {

    private static BootstrapApplication instance;
    private BootstrapComponent component;

    /**
     * Create main application
     */
    public BootstrapApplication() {
    }


    @Override
    public void onCreate() {
        super.onCreate();

        init();

        instance = this;

        // Perform injection
        //Injector.init(this, )
        component = DaggerComponentInitializer.init();

        initializeApplication();

        onAfterInjection();
    }

    private void initializeApplication() {

        // Initialize the AWS Mobile Client
        AWSMobileClient.initializeMobileClientIfNecessary(getApplicationContext());

        // ... Put any application-specific initialization logic here ...
    }


    public static BootstrapComponent component() {
        return instance.component;
    }

    protected abstract void onAfterInjection();

    protected abstract void init();

    public static BootstrapApplication getInstance() {
        return instance;
    }

    public BootstrapComponent getComponent() {
        return component;
    }

    public final static class DaggerComponentInitializer {

        public static BootstrapComponent init() {
            return DaggerBootstrapComponent.builder()
                    .androidModule(new AndroidModule())
                    .bootstrapModule(new BootstrapModule())
                    .build();
        }

    }
}
