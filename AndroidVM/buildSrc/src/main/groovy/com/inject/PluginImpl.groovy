package com.inject;

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class PluginImpl implements Plugin<Project> {
    public void apply(Project project) {
        /**
         * 注册transform接口
         */
        def isApp = project.plugins.hasPlugin(AppPlugin)
        if (isApp) {
            def android = project.extensions.getByType(AppExtension)
            def transform = new PreDexTransform(project)
            android.registerTransform(transform)
        }
    }
}