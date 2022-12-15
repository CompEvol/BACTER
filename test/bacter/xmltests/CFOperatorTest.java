/*
 * Copyright (C) 2015 Tim Vaughan <tgvaughan@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package bacter.xmltests;

import bacter.TestBase;
import beast.base.util.Randomizer;
import beast.base.parser.XMLParser;
import org.junit.Test;
import test.beast.beast2vs1.trace.Expectation;
import test.beast.beast2vs1.trace.LogAnalyser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
public class CFOperatorTest extends TestBase {

    @Test
    public void testCFUniform() throws Exception {
        Randomizer.setSeed(1);

        XMLParser parser = new XMLParser();
        beast.base.inference.Runnable runnable = parser.parseFile(
                new File("examples/CFOperatorTests/CFUniformTest5taxon.xml"));
        setupTestLoggers(runnable);
        runnable.run();

        List<Expectation> expectations = new ArrayList<>();
        expectations.add(new Expectation("acg.CFheight", 1.606, 0.2));
        expectations.add(new Expectation("acg.CFlength", 4.181, 0.5));
        expectations.add(new Expectation("acg.nConv", 21.0, 0.5));

        new LogAnalyser("CFUniformTest5taxon.stats", expectations);

        for (Expectation expectation : expectations) {
            assertTrue(expectation.isValid());
            assertTrue(expectation.isPassed());
        }

        Files.deleteIfExists(Paths.get("CFUniformTest5taxon.stats"));
        Files.deleteIfExists(Paths.get("CFUniformTest5taxon.converted"));
        Files.deleteIfExists(Paths.get("CFUniformTest5taxon.trees"));
        Files.deleteIfExists(Paths.get("CFUniformTest5taxon.cf"));
        Files.deleteIfExists(Paths.get("CFUniformTest5taxon.xml.state"));
    }


    @Test
    public void testCFWB() throws Exception {
        Randomizer.setSeed(1);

        XMLParser parser = new XMLParser();
        beast.base.inference.Runnable runnable = parser.parseFile(
                new File("examples/CFOperatorTests/CFWilsonBaldingTest5taxon.xml"));
        setupTestLoggers(runnable);
        runnable.run();

        List<Expectation> expectations = new ArrayList<>();
        expectations.add(new Expectation("acg.CFheight", 1.606, 0.2));
        expectations.add(new Expectation("acg.CFlength", 4.181, 0.5));
        expectations.add(new Expectation("acg.nConv", 21.0, 0.5));

        new LogAnalyser("CFWilsonBaldingTest5taxon.stats", expectations);

        for (Expectation expectation : expectations) {
            assertTrue(expectation.isValid());
            assertTrue(expectation.isPassed());
        }

        Files.deleteIfExists(Paths.get("CFWilsonBaldingTest5taxon.stats"));
        Files.deleteIfExists(Paths.get("CFWilsonBaldingTest5taxon.converted"));
        Files.deleteIfExists(Paths.get("CFWilsonBaldingTest5taxon.trees"));
        Files.deleteIfExists(Paths.get("CFWilsonBaldingTest5taxon.cf"));
        Files.deleteIfExists(Paths.get("CFWilsonBaldingTest5taxon.xml.state"));
    }

    @Test
    public void testCFCS() throws Exception {
        Randomizer.setSeed(1);

        XMLParser parser = new XMLParser();
        beast.base.inference.Runnable runnable = parser.parseFile(
                new File("examples/CFOperatorTests/CFConversionSwapTest5taxon.xml"));
        setupTestLoggers(runnable);
        runnable.run();

        List<Expectation> expectations = new ArrayList<>();
        expectations.add(new Expectation("acg.CFheight", 1.606, 0.2));
        expectations.add(new Expectation("acg.CFlength", 4.181, 0.5));
        expectations.add(new Expectation("acg.nConv", 21.0, 0.5));

        new LogAnalyser("CFConversionSwapTest5taxon.stats", expectations);

        for (Expectation expectation : expectations) {
            assertTrue(expectation.isValid());
            assertTrue(expectation.isPassed());
        }

        Files.deleteIfExists(Paths.get("CFConversionSwapTest5taxon.stats"));
        Files.deleteIfExists(Paths.get("CFConversionSwapTest5taxon.converted"));
        Files.deleteIfExists(Paths.get("CFConversionSwapTest5taxon.trees"));
        Files.deleteIfExists(Paths.get("CFConversionSwapTest5taxon.cf"));
        Files.deleteIfExists(Paths.get("CFConversionSwapTest5taxon.xml.state"));
    }

    @Test
    public void testCFSTS() throws Exception {
        Randomizer.setSeed(1);

        XMLParser parser = new XMLParser();
        beast.base.inference.Runnable runnable = parser.parseFile(
                new File("examples/CFOperatorTests/CFSubtreeSlideTest5taxon.xml"));
        setupTestLoggers(runnable);
        runnable.run();

        List<Expectation> expectations = new ArrayList<>();
        expectations.add(new Expectation("acg.CFheight", 1.606, 0.2));
        expectations.add(new Expectation("acg.CFlength", 4.181, 0.5));
        expectations.add(new Expectation("acg.nConv", 21.0, 0.5));

        new LogAnalyser("CFSubtreeSlideTest5taxon.stats", expectations);

        for (Expectation expectation : expectations) {
            assertTrue(expectation.isValid());
            assertTrue(expectation.isPassed());
        }

        Files.deleteIfExists(Paths.get("CFSubtreeSlideTest5taxon.stats"));
        Files.deleteIfExists(Paths.get("CFSubtreeSlideTest5taxon.trees"));
        Files.deleteIfExists(Paths.get("CFSubtreeSlideTest5taxon.xml.state"));
    }

    @Test
    public void testCFSTX() throws Exception {
        Randomizer.setSeed(1);

        XMLParser parser = new XMLParser();
        beast.base.inference.Runnable runnable = parser.parseFile(
                new File("examples/CFOperatorTests/CFSubtreeExchangeTest5taxon.xml"));
        setupTestLoggers(runnable);
        runnable.run();

        List<Expectation> expectations = new ArrayList<>();
        expectations.add(new Expectation("acg.CFheight", 1.606, 0.2));
        expectations.add(new Expectation("acg.CFlength", 4.181, 0.5));
        expectations.add(new Expectation("acg.nConv", 21.0, 0.5));

        new LogAnalyser("CFSubtreeExchangeTest5taxon.stats", expectations);

        for (Expectation expectation : expectations) {
            assertTrue(expectation.isValid());
            assertTrue(expectation.isPassed());
        }

        Files.deleteIfExists(Paths.get("CFSubtreeExchangeTest5taxon.stats"));
        Files.deleteIfExists(Paths.get("CFSubtreeExchangeTest5taxon.trees"));
        Files.deleteIfExists(Paths.get("CFSubtreeExchangeTest5taxon.xml.state"));
    }
}
