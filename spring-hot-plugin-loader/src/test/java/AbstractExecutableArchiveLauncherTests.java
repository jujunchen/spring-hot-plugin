/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import csdn.itsaysay.plugin.loader.archive.Archive;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/**
 * 用来创建临时测试的jar包
 */
public abstract class AbstractExecutableArchiveLauncherTests {

	@TempDir
	File tempDir;

	protected File createJarArchive(String name, String entryPrefix) throws IOException {
		return createJarArchive(name, entryPrefix, false, Collections.emptyList());
	}

	@SuppressWarnings("resource")
	protected File createJarArchive(String name, String entryPrefix, boolean indexed, List<String> extraLibs)
			throws IOException {
		return createJarArchive(name, null, entryPrefix, indexed, extraLibs);
	}

	@SuppressWarnings("resource")
	protected File createJarArchive(String name, Manifest manifest, String entryPrefix, boolean indexed,
			List<String> extraLibs) throws IOException {
		File archive = new File(this.tempDir, name);
		JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(archive));
		if (manifest != null) {
			jarOutputStream.putNextEntry(new JarEntry("META-INF/"));
			jarOutputStream.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
			manifest.write(jarOutputStream);
			jarOutputStream.closeEntry();
		}
		jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/"));
		jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/classes/"));
		jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/lib/"));
		if (indexed) {
			jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/classpath.idx"));
			Writer writer = new OutputStreamWriter(jarOutputStream, StandardCharsets.UTF_8);
			writer.write("- \"" + entryPrefix + "/lib/foo.jar\"\n");
			writer.write("- \"" + entryPrefix + "/lib/bar.jar\"\n");
			writer.write("- \"" + entryPrefix + "/lib/baz.jar\"\n");
			writer.flush();
			jarOutputStream.closeEntry();
		}
		addNestedJars(entryPrefix, "/lib/foo.jar", jarOutputStream);
		addNestedJars(entryPrefix, "/lib/bar.jar", jarOutputStream);
		addNestedJars(entryPrefix, "/lib/baz.jar", jarOutputStream);
		for (String lib : extraLibs) {
			addNestedJars(entryPrefix, "/lib/" + lib, jarOutputStream);
		}
		jarOutputStream.close();
		return archive;
	}

	private void addNestedJars(String entryPrefix, String lib, JarOutputStream jarOutputStream) throws IOException {
		JarEntry libFoo = new JarEntry(entryPrefix + lib);
		libFoo.setMethod(ZipEntry.STORED);
		ByteArrayOutputStream fooJarStream = new ByteArrayOutputStream();
		new JarOutputStream(fooJarStream).close();
		libFoo.setSize(fooJarStream.size());
		CRC32 crc32 = new CRC32();
		crc32.update(fooJarStream.toByteArray());
		libFoo.setCrc(crc32.getValue());
		jarOutputStream.putNextEntry(libFoo);
		jarOutputStream.write(fooJarStream.toByteArray());
	}

	protected File explode(File archive) throws IOException {
		File exploded = new File(this.tempDir, "exploded");
		exploded.mkdirs();
		JarFile jarFile = new JarFile(archive);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			File entryFile = new File(exploded, entry.getName());
			if (entry.isDirectory()) {
				entryFile.mkdirs();
			}
			else {
				FileCopyUtils.copy(jarFile.getInputStream(entry), new FileOutputStream(entryFile));
			}
		}
		jarFile.close();
		return exploded;
	}

	protected Set<URL> getUrls(List<Archive> archives) throws MalformedURLException {
		Set<URL> urls = new LinkedHashSet<>(archives.size());
		for (Archive archive : archives) {
			urls.add(archive.getUrl());
		}
		return urls;
	}

	protected final URL toUrl(File file) {
		try {
			return file.toURI().toURL();
		}
		catch (MalformedURLException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
