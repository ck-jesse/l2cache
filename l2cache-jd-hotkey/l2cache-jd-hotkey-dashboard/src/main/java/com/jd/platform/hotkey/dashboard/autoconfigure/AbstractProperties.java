package com.jd.platform.hotkey.dashboard.autoconfigure;

/**
 * User:  fuxueliang
 * Date:  16/8/25
 * Email: fuxueliang@jd.com
 */
public abstract class AbstractProperties {
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
