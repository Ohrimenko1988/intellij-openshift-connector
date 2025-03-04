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
package org.jboss.tools.intellij.openshift.utils.odo;

import com.redhat.devtools.intellij.common.utils.ExecHelper;
import org.fest.util.Files;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;

import static org.jboss.tools.intellij.openshift.Constants.DebugStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class OdoCliComponentTest extends OdoCliTest {
    private boolean push;

    private String project;
    private String application;
    private String component;
    private String service;
    private String storage;
    private String host;

    public OdoCliComponentTest(boolean push) {
        this.push = push;
    }

    @Parameterized.Parameters(name = "pushed: {0}")
    public static Iterable<? extends Object> data() {
        return Arrays.asList(false, true);
    }

    @Before
    public void initTestEnv() {
        project = PROJECT_PREFIX + random.nextInt();
        application = APPLICATION_PREFIX + random.nextInt();
        component = COMPONENT_PREFIX + random.nextInt();
        service = SERVICE_PREFIX + random.nextInt();
        storage = STORAGE_PREFIX + random.nextInt();
        host = odo.getMasterUrl().getHost();
    }

    @Test
    public void checkCreateComponent() throws IOException {
        try {
            createComponent(project, application, component, push);
            List<Component> components = odo.getComponents(project, application);
            assertNotNull(components);
            assertEquals(push ? 1 : 0, components.size());
        } finally {
            odo.deleteProject(project);
        }
    }


    @Test
    public void checkCreateAndDiscoverComponent() throws IOException {
        try {
            createComponent(project, application, component, push);
            List<ComponentDescriptor> components = odo.discover(COMPONENT_PATH);
            assertNotNull(components);
            assertEquals(1, components.size());
            assertEquals(new File(COMPONENT_PATH).getAbsolutePath(), components.get(0).getPath());
            assertEquals(component, components.get(0).getName());
            assertEquals(application, components.get(0).getApplication());
            assertEquals(project, components.get(0).getProject());
        } finally {
            odo.deleteProject(project);
        }
    }

    @Test
    public void checkCreateAndDeleteComponent() throws IOException {
        try {
            createComponent(project, application, component, push);
            odo.deleteComponent(project, application, COMPONENT_PATH, component);
        } finally {
            odo.deleteProject(project);
        }
    }

    private void checkCreateComponentAndCreateURL(boolean secure) throws IOException {
        try {
            createComponent(project, application, component, push);
            odo.createURL(project, application, COMPONENT_PATH, component, "url1", 8080, secure, host);
            List<URL> urls = odo.listURLs(project, application, COMPONENT_PATH, component);
            assertEquals(odo.isOpenShift() ? 2 : 1, urls.size());
        } finally {
            odo.deleteProject(project);
        }
    }

    @Test
    public void checkCreateComponentAndCreateURL() throws IOException {
        checkCreateComponentAndCreateURL(false);
    }

    @Test
    public void checkCreateComponentAndCreateSecureURL() throws IOException {
        checkCreateComponentAndCreateURL(true);
    }

    private void checkCreateComponentAndCreateAndDeleteURL(boolean secure) throws IOException {
        try {
            List<URL> urls;
            createComponent(project, application, component, push);
            if (odo.isOpenShift()) {
                // remove url created automatically for openshift cluster to better visibility of the test
                urls = odo.listURLs(project, application, COMPONENT_PATH, component);
                assertEquals(1, urls.size());
                odo.deleteURL(project, application, COMPONENT_PATH, component, urls.get(0).getName());
                if (push) {
                    odo.push(project, application, COMPONENT_PATH, component);
                }
                urls = odo.listURLs(project, application, COMPONENT_PATH, component);
                assertEquals(0, urls.size());
            }

            odo.createURL(project, application, COMPONENT_PATH, component, null, 8080, secure, host);
            urls = odo.listURLs(project, application, COMPONENT_PATH, component);

            assertEquals(1, urls.size());
            assertEquals(URL.State.NOT_PUSHED, urls.get(0).getState());
            if (push) {
                odo.push(project, application, COMPONENT_PATH, component);
                urls = odo.listURLs(project, application, COMPONENT_PATH, component);
                assertEquals(1, urls.size());
                assertEquals(URL.State.PUSHED, urls.get(0).getState());
            }

            odo.deleteURL(project, application, COMPONENT_PATH, component, urls.get(0).getName());
            urls = odo.listURLs(project, application, COMPONENT_PATH, component);

            if (push) {
                assertEquals(1, urls.size());
                assertEquals(URL.State.LOCALLY_DELETED, urls.get(0).getState());
                odo.push(project, application, COMPONENT_PATH, component);
                urls = odo.listURLs(project, application, COMPONENT_PATH, component);
                assertEquals(0, urls.size());
            } else {
                assertEquals(0, urls.size());
            }

        } finally {
            odo.deleteProject(project);
        }
    }

    @Test
    public void checkCreateComponentAndCreateAndDeleteURL() throws IOException {
        checkCreateComponentAndCreateAndDeleteURL(false);
    }

    @Test
    public void checkCreateComponentAndCreateAndDeleteSecureURL() throws IOException {
        checkCreateComponentAndCreateAndDeleteURL(true);
    }

    @Test
    public void checkCreateComponentAndLinkService() throws IOException {
        Assume.assumeTrue(push);
        try {
            createComponent(project, application, component, push);
            ServiceTemplate serviceTemplate = getServiceTemplate();
            OperatorCRD crd = getOperatorCRD(serviceTemplate);
            odo.createService(project, application, serviceTemplate, crd, service, null, true);
            List<Service> deployedServices = odo.getServices(project, application);
            assertNotNull(deployedServices);
            assertEquals(1, deployedServices.size());
            Service deployedService = deployedServices.get(0);
            assertNotNull(deployedService);
            odo.link(project, application, COMPONENT_PATH, component, deployedService.getKind()+"/"+deployedService.getName());
            odo.push(project,application, COMPONENT_PATH, component);
        } finally {
            odo.deleteProject(project);
        }
    }

    @Test
    public void checkCreateComponentAndCreateStorage() throws IOException {
        try {
            createStorage(project, application, component, push, storage);
        } finally {
            odo.deleteProject(project);
        }
    }

    @Test
    public void checkCreateComponentAndCreateDeleteStorage() throws IOException {
        try {
            createStorage(project, application, component, push, storage);
            odo.deleteStorage(project, application, COMPONENT_PATH, component, storage);
        } finally {
            odo.deleteProject(project);
        }
    }

    @Test
    public void checkCreateComponentAndListURLs() throws IOException {
        try {
            createComponent(project, application, component, push);
            List<URL> urls = odo.listURLs(project, application, COMPONENT_PATH, component);
            assertEquals(odo.isOpenShift() ? 1 : 0, urls.size());
            if (odo.isOpenShift()) {
                assertEquals(push ? URL.State.PUSHED : URL.State.NOT_PUSHED, urls.get(0).getState());
            }
        } finally {
            odo.deleteProject(project);
        }
    }

    @Test
    public void checkCreateComponentAndDebug() throws IOException {
        Assume.assumeTrue(push);
        try {
            createComponent(project, application, component, push);
            odo.createURL(project, application, COMPONENT_PATH, component, "url1", 8080, false, host);
            odo.push(project, application, COMPONENT_PATH, component);
            List<URL> urls = odo.listURLs(project, application, COMPONENT_PATH, component);
            assertEquals(odo.isOpenShift() ? 2 : 1, urls.size());
            int debugPort;
            try (ServerSocket serverSocket = new ServerSocket(0)) {
                debugPort = serverSocket.getLocalPort();
            }
            ExecHelper.submit(() -> {
                try {
                    odo.debug(project, application, COMPONENT_PATH, component, debugPort);
                    DebugStatus status = odo.debugStatus(project, application, COMPONENT_PATH, component);
                    assertEquals(DebugStatus.RUNNING, status);
                } catch (IOException e) {
                    fail("Should not raise Exception");
                }
            });

        } finally {
            odo.deleteProject(project);
        }
    }

    @Test
    public void checkCreateComponentStarter() throws IOException {
        try {
            createProject(project);
            odo.createComponent(project, application, "java-springboot", REGISTRY_NAME, component, Files.newTemporaryFolder().getAbsolutePath(), null, "springbootproject", push);
            List<Component> components = odo.getComponents(project, application);
            assertNotNull(components);
            assertEquals(push ? 1 : 0, components.size());
        } finally {
            odo.deleteProject(project);
        }
    }
}
