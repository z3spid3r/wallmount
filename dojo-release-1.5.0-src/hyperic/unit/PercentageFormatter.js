/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2011], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

dojo.provide("hyperic.unit.PercentageFormatter");

dojo.require("hyperic.unit.SimpleFormatter");
dojo.require("dojo.number");

dojo.declare("hyperic.unit.PercentageFormatter",
    [ hyperic.unit.SimpleFormatter ],{
        
    formatNumber: function(/*Number*/rawValue, f, /*Object*/format, /*String*/locale){
        return f(rawValue * this.getMultiplier(), format) + '%';
    },
    
    getMultiplier: function(){
    	return 100.0;
    },
    
    parseValue: function(/*String*/rawValue, f){
        return f(rawValue,{type:'percent'})*(100/this.getMultiplier());
    },

    parse: function(/*String*/val, unitType, /*String*/locale){
        // summary:
        var parsedNumber = this.parseValue(val,this.getParser());
        return new hyperic.unit.UnitNumber({value: parsedNumber,
                                            units: hyperic.unit.UnitsConstants.UNIT_PERCENTAGE,
                                            scale: hyperic.unit.UnitsConstants.SCALE_NONE});
    },
    
        
});