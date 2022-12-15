/*
 * Copyright (C) 2014 Tim Vaughan <tgvaughan@gmail.com>
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

package bacter.operators;

import bacter.Conversion;
import beast.base.core.Description;
import beast.base.evolution.tree.Node;
import beast.base.util.Randomizer;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("Cause recombinant edge to hop between clonal frame edges.")
public class ConvertedEdgeHop extends ACGOperator {

    public ConvertedEdgeHop() { }
    
    @Override
    public double proposal() {

        if (acg.getTotalConvCount()==0)
            return Double.NEGATIVE_INFINITY;
        
        // Select recombination at random
        Conversion recomb = chooseConversion();
        
        // Choose whether to move departure or arrival point
        boolean moveDeparture;
        moveDeparture = recomb.getNode2().isRoot() || Randomizer.nextBoolean();
        
        // Select new attachment point:
        double u = Randomizer.nextDouble()*acg.getClonalFrameLength();
        Node nodeBelow = null;
        double newHeight = -1;
        for (Node node : acg.getNodesAsArray()) {
            if (node.isRoot())
                continue;
            
            if (u<node.getLength()) {
                newHeight = node.getHeight() + u;
                nodeBelow = node;
                break;
            }
            
            u -= node.getLength();
        }
        
        if (newHeight < 0.0)
            throw new IllegalStateException("Problem with recombinant edge "
                    + "hop operator!  This is a bug.");
        
        // Check that new height does not lie out of bounds
        if (moveDeparture) {
            if (newHeight>recomb.getHeight2())
                return Double.NEGATIVE_INFINITY;
            else {
                recomb.setHeight1(newHeight);
                recomb.setNode1(nodeBelow);
            }
        } else {
            if (newHeight<recomb.getHeight1())
                return Double.NEGATIVE_INFINITY;
            else {
                recomb.setHeight2(newHeight);
                recomb.setNode2(nodeBelow);
            }
        }

        assert !acg.isInvalid() : "CEH produced invalid state.";
        
        return 0.0;
    }
    
}
