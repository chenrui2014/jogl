/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl.test.junit.jogl.glsl;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquare0;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.io.IOException;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLUniformData;

import org.junit.Assert;
import org.junit.Test;

/**
 * Testing different vertex attribute (VA) data sets on one shader
 * and shader state in general.
 */
public class TestGLSLShaderState02NEWT extends UITestCase {
    static long durationPerTest = 10; // ms
    static int frames_perftest =  5000; // frames
    static int frames_warmup   =   100; // frames

    static final int vertices0_loc = 1;
    static final int colors0_loc = 2;
    
    @Test
    public void testShaderState01Validation() throws InterruptedException {
        // preset ..
        GLWindow window = GLSLMiscHelper.createWindow();
        GLContext context = window.getContext();
        context.setSynchronized(true);
        context.makeCurrent();
        GL2ES2 gl = context.getGL().getGL2ES2();

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        // test code ..        
        ShaderState st = new ShaderState();
        
        ShaderCode rsVp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "RedSquareShader");
        ShaderCode rsFp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "RedSquareShader");
        ShaderCode rsFp1 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "RedSquareShader2");

        ShaderProgram sp1 = new ShaderProgram();
        sp1.add(rsVp0);
        sp1.add(rsFp1);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertTrue(0>sp1.program());        
        sp1.init(gl);
        Assert.assertTrue(0<=sp1.program()); 
        Assert.assertTrue(!sp1.inUse());
        Assert.assertTrue(!sp1.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertTrue(sp1.link(gl, System.err));
        Assert.assertTrue(sp1.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        ShaderProgram sp0 = new ShaderProgram();        
        sp0.add(rsVp0);
        sp0.add(rsFp0);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        Assert.assertTrue(0>sp0.program());        
        sp0.init(gl);
        Assert.assertTrue(0<=sp0.program()); 
        Assert.assertTrue(!sp0.inUse());
        Assert.assertTrue(!sp0.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());        
                
        st.attachShaderProgram(gl, sp0);
        
        // Allocate Vertex Array0
        GLArrayDataServer vertices0 = GLSLMiscHelper.createRSVertices0(gl, st, vertices0_loc);
        System.err.println("vertices0: " + vertices0);        
        vertices0.enableBuffer(gl, false);
        Assert.assertEquals(vertices0_loc, vertices0.getLocation());
        
        // Allocate Color Array0
        GLArrayDataServer colors0 = GLSLMiscHelper.createRSColors0(gl, st, colors0_loc);
        System.err.println("colors0: " + colors0);
        colors0.enableBuffer(gl, false);
        Assert.assertEquals(colors0_loc, colors0.getLocation());
                               
        Assert.assertTrue(sp0.link(gl, System.err));
        Assert.assertTrue(sp0.linked());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        Assert.assertEquals(vertices0_loc, vertices0.getLocation());
        Assert.assertEquals(vertices0_loc, st.glGetAttribLocation(gl, vertices0.getName()));
        Assert.assertEquals(vertices0_loc, gl.glGetAttribLocation(st.shaderProgram().program(), vertices0.getName()));
        
        Assert.assertEquals(colors0_loc, colors0.getLocation());
        Assert.assertEquals(colors0_loc, st.glGetAttribLocation(gl, colors0.getName()));
        Assert.assertEquals(colors0_loc, gl.glGetAttribLocation(st.shaderProgram().program(), colors0.getName()));
        
        Assert.assertEquals(null, ShaderState.getShaderState(gl));
        st.glUseProgram(gl, true);
        Assert.assertTrue(sp0.inUse());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertEquals(st, ShaderState.getShaderState(gl));        
        
        // setup mgl_PMVMatrix
        PMVMatrix pmvMatrix = new PMVMatrix();
        GLUniformData pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        st.glUniform(gl, pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertEquals(pmvMatrixUniform, st.getUniform("mgl_PMVMatrix"));
        
        // Allocate Vertex Array1
        GLArrayDataServer vertices1 = GLSLMiscHelper.createRSVertices1(gl, st);
        System.err.println("vertices1: " + vertices1);
        vertices1.enableBuffer(gl, false);
        
        // Allocate Color Array1
        GLArrayDataServer colors1 = GLSLMiscHelper.createRSColors1(gl, st);
        System.err.println("colors1: " + colors1);
        colors1.enableBuffer(gl, false);
        
        // misc GL setup
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        // reshape
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0F, (float) window.getWidth() / (float) window.getHeight(), 1.0F, 100.0F);
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        st.glUniform(gl, pmvMatrixUniform);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        // display #1 vertices0 / colors0 (post-disable)
        GLSLMiscHelper.displayVCArrays(window, gl, true, vertices0, colors0, true, 1, durationPerTest);

        // display #2 #1 vertices1 / colors1 (post-disable)
        GLSLMiscHelper.displayVCArrays(window, gl, true, vertices1, colors1, true, 2, durationPerTest);
        
        // display #3 vertices0 / colors0 (post-disable)
        GLSLMiscHelper.displayVCArrays(window, gl, true, vertices0, colors0, true, 3, durationPerTest);

        // SP1
        // both are currently not attached to ShaderState, hence we have to reset their location as well
        vertices0.setLocation(-1);
        colors0.setLocation(-1);
        
        st.attachShaderProgram(gl, sp1);
        
        // display #1 vertices0 / colors0 (post-disable)
        GLSLMiscHelper.displayVCArrays(window, gl, true, vertices0, colors0, true, 10, durationPerTest);

        // display #2 #1 vertices1 / colors1 (post-disable)
        GLSLMiscHelper.displayVCArrays(window, gl, true, vertices1, colors1, true, 20, durationPerTest);
        
        // display #3 vertices0 / colors0 (post-disable)
        GLSLMiscHelper.displayVCArrays(window, gl, true, vertices0, colors0, true, 30, durationPerTest);
        
        // cleanup
        vertices1.destroy(gl);
        vertices0.destroy(gl);
        colors0.destroy(gl);
        colors1.destroy(gl);
        st.glUseProgram(gl, false);
        sp0.release(gl, true);
        context.release();
        window.destroy();        
    }

    @Test
    public void testShaderState01PerformanceDouble() throws InterruptedException {
        // preset ..
        GLWindow window = GLSLMiscHelper.createWindow();
        GLContext context = window.getContext();
        context.setSynchronized(true);
        context.makeCurrent();
        GL2ES2 gl = context.getGL().getGL2ES2();

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        // test code ..        
        ShaderState st = new ShaderState();
        
        ShaderCode rsVp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "RedSquareShader");
        ShaderCode rsFp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "RedSquareShader");
        ShaderCode rsFp1 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, RedSquare0.class,
                "shader", "shader/bin", "RedSquareShader2");

        ShaderProgram sp1 = new ShaderProgram();
        sp1.add(rsVp0);
        sp1.add(rsFp1);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        Assert.assertTrue(0>sp1.program());        
        sp1.init(gl);
        Assert.assertTrue(sp1.link(gl, System.err));
        
        ShaderProgram sp0 = new ShaderProgram();
        sp0.add(rsVp0);
        sp0.add(rsFp0);
        
        sp0.init(gl);
        Assert.assertTrue(sp0.link(gl, System.err));
        
        st.attachShaderProgram(gl, sp0);        
        st.glUseProgram(gl, true);
        
        // setup mgl_PMVMatrix
        PMVMatrix pmvMatrix = new PMVMatrix();
        GLUniformData pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());        
        st.glUniform(gl, pmvMatrixUniform);
        
        // Allocate Vertex Array0
        GLArrayDataServer vertices0 = GLSLMiscHelper.createRSVertices0(gl, st, -1); 
        vertices0.enableBuffer(gl, false);
        
        // Allocate Vertex Array1
        GLArrayDataServer vertices1 = GLSLMiscHelper.createRSVertices1(gl, st); 
        vertices1.enableBuffer(gl, false);
        
        // Allocate Color Array0
        GLArrayDataServer colors0 = GLSLMiscHelper.createRSColors0(gl, st, -1);
        colors0.enableBuffer(gl, false);
        
        // Allocate Color Array1
        GLArrayDataServer colors1 = GLSLMiscHelper.createRSColors1(gl, st);
        colors1.enableBuffer(gl, false);
        
        // misc GL setup
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);

        // reshape
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0F, (float) window.getWidth() / (float) window.getHeight(), 1.0F, 100.0F);
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, -10);
        st.glUniform(gl, pmvMatrixUniform);

        // validation ..
        GLSLMiscHelper.displayVCArrays(window, gl, true, vertices0, colors0, true, 1, 0);
        GLSLMiscHelper.displayVCArrays(window, gl, true, vertices1, colors1, true, 2, 0);
        
        long t0 = System.currentTimeMillis();
        int frames;
        // warmup ..
        for(frames=0; frames<frames_warmup; frames++) {
            GLSLMiscHelper.displayVCArraysNoChecks(window, gl, true, vertices0, colors0, true);
            GLSLMiscHelper.displayVCArraysNoChecks(window, gl, true, vertices1, colors1, true);
        }        
        // measure ..
        for(frames=0; frames<frames_perftest; frames+=2) {
            // SP0
            vertices0.setLocation(-1);
            colors0.setLocation(-1);        
            st.attachShaderProgram(gl, sp0);
            GLSLMiscHelper.displayVCArraysNoChecks(window, gl, true, vertices0, colors0, true);
            GLSLMiscHelper.displayVCArraysNoChecks(window, gl, true, vertices1, colors1, true);
            // SP1
            vertices0.setLocation(-1);
            colors0.setLocation(-1);        
            st.attachShaderProgram(gl, sp1);
            GLSLMiscHelper.displayVCArraysNoChecks(window, gl, true, vertices0, colors0, true);
            GLSLMiscHelper.displayVCArraysNoChecks(window, gl, true, vertices1, colors1, true);
        }
        
        final long t1 = System.currentTimeMillis();
        final long dt = t1 - t0;        
        final double fps = ( frames * 1000.0 ) / (double) dt;
        final String fpsS = String.valueOf(fps);        
        final int fpsSp = fpsS.indexOf('.');        
        System.err.println("testShaderState01PerformanceDouble: "+dt/1000.0 +"s: "+ frames + "f, " + fpsS.substring(0, fpsSp+2) + " fps, "+dt/frames+" ms/f");        
        
        // cleanup
        st.glUseProgram(gl, false);
        sp0.release(gl, true);
        vertices1.destroy(gl);
        vertices0.destroy(gl);
        colors0.destroy(gl);
        colors1.destroy(gl);
        context.release();
        window.destroy();        
    }
    
    public static void main(String args[]) throws IOException {
        System.err.println("main - start");
        boolean wait = false;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atoi(args[++i], (int)durationPerTest);
            }
            if(args[i].equals("-wait")) {
                wait = true;
            }
        }
        if(wait) {
            while(-1 == System.in.read()) ;
        }
        String tstname = TestGLSLShaderState02NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
        System.err.println("main - end");
    }    
}
