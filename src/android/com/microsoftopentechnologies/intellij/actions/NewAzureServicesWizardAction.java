/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoftopentechnologies.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.microsoftopentechnologies.intellij.forms.AzureServicesSelectionForm;
import com.microsoftopentechnologies.intellij.helpers.UIHelper;

public class NewAzureServicesWizardAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        AzureServicesSelectionForm form = new AzureServicesSelectionForm();
        form.setProject(e.getProject());
        form.setModule(e.getData(DataKeys.MODULE));
        UIHelper.packAndCenterJDialog(form);
        form.setVisible(true);
    }
}