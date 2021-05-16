package io.sapl.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import lombok.val;

public class JarPathUtilTest {
    @Test
    public void testGetJarFilePath() {
        val jarFileUrl = "jar:file:/C:/Users/johndoe/jar_file.jar!/directory_in_jar";
        assertThat(JarPathUtil.getJarFilePath(jarFileUrl.split(("!"))), is("/C:/Users/johndoe/jar_file.jar"));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> JarPathUtil.getJarFilePath(new String[]{}));
    }
}

