package csdn.itsaysay.plugin.maven;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.ScopeArtifactFilter;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;


/**
 * 插件打包工具
 */
@Mojo(name = "repackage")
public class PackagePlugin extends AbstractMojo {

    @Parameter(defaultValue = "${project.build.directory}/classes", readonly = true)
    private File classesDir;

    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}.jar", readonly = true)
    private File jarFile;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Component
    private MavenSession mavenSession;



    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        //打包
        try {
            packageJar();
        } catch (Exception ex) {
            getLog().error(ex);
        }
    }

    private void packageJar() throws DependencyResolutionRequiredException, IOException, ManifestException {
        MavenArchiver mavenArchiver = createJarPackage();
        addClasses(mavenArchiver);
        addArtifacts(mavenArchiver);
        writeJarPackage(mavenArchiver);
    }

    private void writeJarPackage(MavenArchiver mavenArchiver) throws DependencyResolutionRequiredException, IOException, ManifestException {
        mavenArchiver.createArchive(mavenSession, project, getJarConfiguration());
    }

    private void addClasses(MavenArchiver mavenArchiver) {
        getLog().info("Start packing the class files......");

        Collection<File> classesList = FileUtils.listFiles(classesDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        for (File file : classesList) {
            mavenArchiver.getArchiver().addFile(file, PackageConstants.CLASS_DIR + getRelativeFilePath(file));
        }
    }

    private String getRelativeFilePath(File file) {
        return StringUtils.substringAfter(file.getAbsolutePath(), PackageConstants.CLASS_DIR);
    }

    private void addArtifacts(MavenArchiver mavenArchiver) {
        getLog().info("Start packing the artifacts files......");

        //依赖
        Set<Artifact> artifacts = filterArtifacts();
        for (Artifact artifact : artifacts) {
            mavenArchiver.getArchiver().addFile(artifact.getFile(), PackageConstants.LIB_DIR + File.separator + artifact.getFile().getName());
        }
    }

    private MavenArchiveConfiguration getJarConfiguration() {
        getLog().info("Start packing the Manifest file......");

        MavenArchiveConfiguration jarConfiguration = new MavenArchiveConfiguration();
        jarConfiguration.addManifestEntry(PackageConstants.PLUGIN_ID, project.getModel().getArtifactId());
        jarConfiguration.addManifestEntry(PackageConstants.PLUGIN_VERSION, project.getModel().getVersion());
        jarConfiguration.addManifestEntry(PackageConstants.PLUGIN_DES, project.getModel().getDescription());
        jarConfiguration.addManifestEntry(PackageConstants.DEVELOPER_NAME, String.join(";", project.getModel().getDevelopers().toArray(new String[0])));

        jarConfiguration.setCompress(false);
        return jarConfiguration;
    }

    private MavenArchiver createJarPackage() {
        MavenArchiver mavenArchiver = new MavenArchiver();
        mavenArchiver.setOutputFile(jarFile);
        mavenArchiver.setArchiver(new JarArchiver());
        return mavenArchiver;
    }


    private Set<Artifact> filterArtifacts() {
        ScopeArtifactFilter scopeArtifactFilter = new ScopeArtifactFilter();
        scopeArtifactFilter.setIncludeRuntimeScopeWithImplications(true);
        project.setArtifactFilter(scopeArtifactFilter);
        return project.getArtifacts();
    }
}
