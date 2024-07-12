import csdn.itsaysay.plugin.loader.JarLauncher;
import csdn.itsaysay.plugin.loader.archive.Archive;
import csdn.itsaysay.plugin.loader.archive.JarFileArchive;
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
        File jarRoot = createJarArchive("archive.jar", "BOOT-INF");
        try (JarFileArchive archive = new JarFileArchive(jarRoot)) {
            JarLauncher launcher = new JarLauncher(archive);
            List<Archive> classPathArchives = new ArrayList<>();
            launcher.getClassPathArchivesIterator().forEachRemaining(classPathArchives::add);
            assertThat(classPathArchives).hasSize(4);
            assertThat(getUrls(classPathArchives)).containsOnly(
                    new URL("jar:" + jarRoot.toURI().toURL() + "!/BOOT-INF/classes!/"),
                    new URL("jar:" + jarRoot.toURI().toURL() + "!/BOOT-INF/lib/foo.jar!/"),
                    new URL("jar:" + jarRoot.toURI().toURL() + "!/BOOT-INF/lib/bar.jar!/"),
                    new URL("jar:" + jarRoot.toURI().toURL() + "!/BOOT-INF/lib/baz.jar!/"));
            for (Archive classPathArchive : classPathArchives) {
                classPathArchive.close();
            }
        }
    }
}
