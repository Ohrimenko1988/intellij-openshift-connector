package org.jboss.tools.intellij.openshift.utils.common;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.ContainerFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import com.intellij.remoterobot.fixtures.JTreeFixture;
import org.jetbrains.annotations.NotNull;
import java.time.Duration;
import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitFor;

@DefaultXpath(by = "ProjectTreeFixture type", xpath = "//div[@class='ThreeComponentsSplitter']//div[@class='InternalDecorator']")
@FixtureName(name = "Project Tree Component")
public class ProjectTreeFixture extends ContainerFixture{
  public ProjectTreeFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
    super(remoteRobot, remoteComponent);
  }

  public void waitItemInTree(String itemText, int timeout, int pollingTimeout){
    waitFor(Duration.ofSeconds(timeout),
        Duration.ofSeconds(pollingTimeout),
        "The node-js project is not still available.",
        () -> getProjectTree().hasText(itemText));
  }

  private JTreeFixture getProjectTree(){
    return find(JTreeFixture.class, byXpath("//div[@class='ProjectViewTree']"), Duration.ofSeconds(60));
  }

}
