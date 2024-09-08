import vip.aliali.spring.plugin.loader.JarLauncher;
import vip.aliali.spring.plugin.loader.archive.Archive;
import vip.aliali.spring.plugin.loader.archive.JarFileArchive;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Jar 包读取测试类
 * @author 阿提说说
 */
public class JarLauncherTest extends AbstractExecutableArchiveLauncherTests {

    @Test
    public void test() throws Exception {
        File jarRoot = createJarArchive("archive.jar");
        try (JarFileArchive archive = new JarFileArchive(jarRoot)) {
            JarLauncher launcher = new JarLauncher(archive);
            List<Archive> classPathArchives = new ArrayList<>();
            launcher.getClassPathArchivesIterator().forEachRemaining(classPathArchives::add);
            assertThat(classPathArchives).hasSize(4);
            assertThat(getUrls(classPathArchives)).containsOnly(
                    new URL("jar:" + jarRoot.toURI().toURL() + "!/classes!/"),
                    new URL("jar:" + jarRoot.toURI().toURL() + "!/lib/foo.jar!/"),
                    new URL("jar:" + jarRoot.toURI().toURL() + "!/lib/bar.jar!/"),
                    new URL("jar:" + jarRoot.toURI().toURL() + "!/lib/baz.jar!/"));
            for (Archive classPathArchive : classPathArchives) {
                classPathArchive.close();
            }
        }
    }
}
