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

package bacter.model.pop;

import bacter.ConversionGraph;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Loggable;
import beast.base.inference.parameter.IntegerParameter;
import beast.base.inference.parameter.RealParameter;
import beast.base.evolution.tree.coalescent.PopulationFunction;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * In BEAST 2, BSP is implemented as a tree distribution rather than
 * a population function.  The population function approach is much
 * more flexible and is directly applicable to BACTER's model.
 *
 * This variant allows one to use a grid of population change times which are
 * evenly distributed between the most recent sample and the root of the clonal
 * frame. This is arguably more appropriate than using the clonal frame
 * coalescence times since conversion-associated coalescences occur more evenly
 * across the tree.
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("Piecewise constant/linear population function.")
public class PiecewisePopulationFunction extends PopulationFunction.Abstract implements Loggable {

    public Input<RealParameter> popSizesInput = new Input<>("popSizes",
            "Population sizes in intervals", Input.Validate.REQUIRED);

    public Input<RealParameter> changeTimesInput = new Input<>("changeTimes",
            "Change times parameter.", Input.Validate.REQUIRED);

    RealParameter popSizes, changeTimes;

    double[] intensities;
    double[] groupBoundaries;

    @Override
    public void initAndValidate() {
        popSizes = popSizesInput.get();
        changeTimes = changeTimesInput.get();

        groupBoundaries = new double[popSizes.getDimension()];
        intensities = new double[popSizes.getDimension()];

        super.initAndValidate();
    }

    @Override
    public List<String> getParameterIds() {
        List<String> ids = new ArrayList<>();
        ids.add(popSizesInput.get().getID());
        ids.add(changeTimesInput.get().getID());

        return ids;
    }

    @Override
    public void prepare() {

        groupBoundaries[0] = 0.0;
        for (int i=1; i<groupBoundaries.length; i++)
            groupBoundaries[i] = changeTimes.getValue(i-1);

        intensities[0] = 0.0;

        for (int i = 1; i < intensities.length; i++) {
            intensities[i] = intensities[i - 1]
                    + (groupBoundaries[i] - groupBoundaries[i-1]) / popSizes.getValue(i - 1);
        }
    }

    @Override
    protected boolean requiresRecalculation() {
        return true;
    }

    @Override
    protected void store() {
        super.store();
    }

    @Override
    protected void restore() {
        super.restore();
    }

    @Override
    public double getPopSize(double t) {
        prepare();

        if (t <= 0)
            return popSizes.getValue(0);

        if (t >= groupBoundaries[groupBoundaries.length-1])
            return popSizes.getValue(popSizes.getDimension()-1);

        int interval = Arrays.binarySearch(groupBoundaries, t);

        if (interval<0)
            interval = -(interval + 1) - 1;  // boundary to the left of time.

        return popSizes.getValue(interval);
    }

    @Override
    public double getIntensity(double t) {
        prepare();

        if (t <= 0 )
            return -t/popSizes.getValue(0);

        if (t >= groupBoundaries[groupBoundaries.length-1])
            return intensities[intensities.length-1]
                    + (t-groupBoundaries[intensities.length-1])
                    /popSizes.getValue(popSizes.getDimension()-1);

        int interval = Arrays.binarySearch(groupBoundaries, t);

        if (interval<0)
            interval = -(interval + 1) - 1; // boundary to the left of time.

        return intensities[interval] + (t-groupBoundaries[interval])/popSizes.getValue(interval);
    }

    @Override
    public double getInverseIntensity(double x) {
        prepare();

        if (x<=0.0)
            return -x*popSizes.getValue(0);

        if (x >= intensities[intensities.length-1])
            return groupBoundaries[groupBoundaries.length-1]
                    + (x - intensities[intensities.length-1])
                    *popSizes.getValue(popSizes.getDimension()-1);

        int interval = Arrays.binarySearch(intensities, x);

        if (interval<0)
            interval = -(interval + 1) - 1; // boundary to the left of x

        return groupBoundaries[interval]
                + (x-intensities[interval])*popSizes.getValue(interval);
    }

    // Loggable implementation:

    @Override
    public void init(PrintStream out) {
        prepare();

        for (int i=0; i<popSizes.getDimension(); i++) {
            out.print(getID() + ".t" + i + "\t");
            out.print(getID() + ".N" + i + "\t");
        }
    }

    @Override
    public void log(long nSample, PrintStream out) {
        prepare();

        for (int i=0; i<popSizes.getDimension(); i++) {
            out.print(groupBoundaries[i] + "\t");
            out.print(popSizes.getValue(i) + "\t");
        }
    }

    @Override
    public void close(PrintStream out) {

    }

    /**
     * Main method for testing.
     *
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) throws Exception {

        String acgString = "[&15,0,1.3905355989030808,31,770,1.597708055397074] " +
                        "[&30,931,2.4351280458424904,36,2486,3.78055549386568] " +
                        "[&15,941,2.0439957300083322,38,2364,6.911056700367016] " +
                        "[&36,1091,4.285505683622974,38,2589,9.867725913197855] " +
                        "((((10:0.5385300170206817,(17:0.116794353049212," +
                        "((3:0.039229346597297564,12:0.039229346597297564)23:0.04582913870888949," +
                        "13:0.08505848530618705)24:0.03173586774302495)26:0.4217356639714697)28:1.8114199763246093," +
                        "((8:0.10883006062265468,2:0.10883006062265468)25:0.556428062025291," +
                        "(6:0.5393311342677402,11:0.5393311342677402)29:0.12592698838020555)31:1.6846918706973453)34:1.4536824928125807," +
                        "(1:0.47184545557390367,14:0.47184545557390367)27:3.331787030583968)37:2.9704369411362554," +
                        "(((15:2.0624287390593707,((16:0.01825347077733299,19:0.01825347077733299)21:0.7668749128372041," +
                        "(7:0.008018731329538273,9:0.008018731329538273)20:0.7771096522849988)32:1.2773003554448337)33:0.7487092404613747," +
                        "4:2.8111379795207454)35:0.1331794525400949,((0:0.0243537216663141," +
                        "5:0.0243537216663141)22:0.5681537100482162,18:0.5925074317145304)30:2.35181000034631)36:3.829751995233287)38:0.0";

        ConversionGraph acg = new ConversionGraph();
        acg.initByName("siteCount", 10000, "fromString", acgString);


        PiecewisePopulationFunction skyline = new PiecewisePopulationFunction();
        skyline.initByName(
                "acg", acg,
                "popSizes", new RealParameter("1.0 1.0 5.0 1.0 2.0"),
                "groupSizes", new IntegerParameter("0"),
                "piecewiseLinear", true);

        try (PrintStream ps = new PrintStream("out.txt")){
            ps.println("t N intensity intensityInv");
            double t = 0.0;
            while (t<10) {
                ps.format("%g %g %g %g\n", t,
                        skyline.getPopSize(t), skyline.getIntensity(t),
                        skyline.getInverseIntensity(skyline.getIntensity(t)));
                t += 0.001;
            }
            ps.close();
        }
    }
}
