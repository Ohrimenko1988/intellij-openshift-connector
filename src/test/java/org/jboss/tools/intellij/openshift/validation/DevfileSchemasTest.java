/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.openshift.validation;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.redhat.devtools.intellij.common.utils.VfsRootAccessHelper;
import org.jetbrains.yaml.schema.YamlJsonSchemaHighlightingInspection;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class DevfileSchemasTest {
    private CodeInsightTestFixture myFixture;

    @Before
    public void setup() throws Exception {
        IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
        TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder(null);
        IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();

        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture, factory.createTempDirTestFixture());

        myFixture.setTestDataPath("src/test/resources");
        myFixture.setUp();
        myFixture.enableInspections(YamlJsonSchemaHighlightingInspection.class);
        String path = new File("src").getAbsoluteFile().getParentFile().getAbsolutePath();
        VfsRootAccessHelper.allowRootAccess(path);
    }

    @After
    public void tearDown() throws Exception {
        myFixture.tearDown();
    }

    private boolean shouldRun() {
        ApplicationInfo info = ApplicationInfo.getInstance();
        return !"2019".equals(info.getMajorVersion()) || !"2".equals(info.getMinorVersion());
    }

    @Test
    public void testQuarkusDevfile() {
        if (shouldRun()) {
            myFixture.configureByFile("devfiles/java-quarkus.yaml");
            myFixture.checkHighlighting();
        } else {
            Assume.assumeFalse(true);
        }
    }

}
