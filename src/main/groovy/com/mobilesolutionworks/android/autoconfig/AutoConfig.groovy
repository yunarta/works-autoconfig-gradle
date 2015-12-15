package com.mobilesolutionworks.android.autoconfig

/**
 * Created by yunarta on 15/12/15.
 */
public class AutoConfig {

    boolean autoConfigure

    int dictateCompileSdk
    String autoCompileSdk

    String dictateAutoBuildTools
    String autoBuildTools

    String dictateAutoRepositoryRevision
    String autoRepositoryRevision

    public int sdk(int version) {
        if (!autoConfigure && dictateCompileSdk != -1) {
            return dictateCompileSdk
        }

        if (autoConfigure && autoCompileSdk != null) {
            version = Integer.parseInt(autoCompileSdk)
        }

        return version
    }

    public String buildTools(String revision) {
        if (!autoConfigure && dictateAutoBuildTools != null) {
            return dictateAutoBuildTools
        }

        if (autoConfigure && autoBuildTools != null) {
            revision = autoBuildTools
        }

        return revision
    }

    public String support(String version) {
        if (!autoConfigure && dictateAutoRepositoryRevision != null) {
            return dictateAutoRepositoryRevision
        }

        if (autoConfigure && autoRepositoryRevision != null) {
            version = autoRepositoryRevision
        }

        return version
    }

}
