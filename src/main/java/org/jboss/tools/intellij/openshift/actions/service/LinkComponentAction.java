/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.openshift.actions.service;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.redhat.devtools.intellij.common.utils.UIHelper;
import org.jboss.tools.intellij.openshift.actions.OdoAction;
import org.jboss.tools.intellij.openshift.tree.application.ApplicationNode;
import org.jboss.tools.intellij.openshift.tree.application.NamespaceNode;
import org.jboss.tools.intellij.openshift.tree.application.ServiceNode;
import org.jboss.tools.intellij.openshift.utils.odo.Component;
import org.jboss.tools.intellij.openshift.utils.odo.ComponentState;
import org.jboss.tools.intellij.openshift.utils.odo.Odo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.jboss.tools.intellij.openshift.Constants.GROUP_DISPLAY_ID;
import static org.jboss.tools.intellij.openshift.telemetry.TelemetryService.TelemetryResult;

public class LinkComponentAction extends OdoAction {
  public LinkComponentAction() {
    super(ServiceNode.class);
  }

  @Override
  protected String getTelemetryActionName() { return "link service to component"; }

  @Override
  public void actionPerformed(AnActionEvent anActionEvent, Object selected, Odo odo) {
    ServiceNode serviceNode = (ServiceNode) selected;
    ApplicationNode applicationNode = serviceNode.getParent();
    NamespaceNode namespaceNode = applicationNode.getParent();
    CompletableFuture.runAsync(() -> {
      try {
        List<Component> components = getTargetComponents(odo, namespaceNode.getName(), applicationNode.getName());
        if (!components.isEmpty()) {
          Component component;
          if (components.size() == 1) {
            component = components.get(0);
          } else {
            String[] componentNames = components.stream().map(Component::getName).toArray(String[]::new);
            String componentName = UIHelper.executeInUI(() -> Messages.showEditableChooseDialog("Link component", "Select component", Messages.getQuestionIcon(), componentNames, componentNames[0], null));
            component = components.get(Arrays.asList(componentNames).indexOf(componentName));
          }
          if (component != null) {
            odo.link(namespaceNode.getName(), applicationNode.getName(), component.getPath(), component.getName(), serviceNode.getName());
            Notifications.Bus.notify(new Notification(GROUP_DISPLAY_ID, "Link component", "Service linked to " + component.getName(),
            NotificationType.INFORMATION));
            sendTelemetryResults(TelemetryResult.SUCCESS);
          } else {
            sendTelemetryResults(TelemetryResult.ABORTED);
          }
       } else {
          String message = "No components to link to";
          sendTelemetryError(message);
          UIHelper.executeInUI(() -> Messages.showWarningDialog(message, "Link component"));
        }
      } catch (IOException e) {
        sendTelemetryError(e);
        UIHelper.executeInUI(() -> Messages.showErrorDialog("Error: " + e.getLocalizedMessage(), "Link component"));
      }
    });
  }

  private List<Component> getTargetComponents(Odo odo, String project, String application) throws IOException {
    return odo.getComponents(project, application).stream().filter(component -> component.getState() == ComponentState.PUSHED).collect(Collectors.toList());
  }
}
