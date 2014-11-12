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

dojo.provide("hyperic.widget.HorizontalArrowPipe");

dojo.require("hyperic.widget.arrowpipe._ArrowPipe");
dojo.require("hyperic.util.FontUtil");
dojo.require("hyperic.unit.UnitsConvert");
dojo.require("dojo.colors");


dojo.declare("hyperic.widget.HorizontalArrowPipe",
    [ hyperic.widget.arrowpipe._ArrowPipe ],{

    _arrowTotalLength: function(){
        return this.width / this.getArrowCount();
    },

    _arrowWidth: function(){
        return this.height - 20;
    },

    _shiftArrows: function(rotation){
        // summary:
        var rLength = this._arrowTotalLength();
        var _s = 0 - rLength + (this.reverse ? -this._shift : this._shift);
        for(var i = 0; i < this._arrows.length; i++) {
            if(!this.reverse)
                this._arrows[i].setTransform({dx: _s});
            else
                this._arrows[i].setTransform({xx: -1, dx: _s+rLength*2});
            _s += rLength;
        }
        
    },
    
    _createArrows: function(rotation){
        // summary:
        //      Creates all arrow shapes used in this
        //      component.
        
        var c0 = new dojo.Color(this._rcolor).toRgba();
        c0[3] = 0.0;
        var c1 = new dojo.Color(this._rcolor).toRgba();
        c1[3] = 1.0;
                
        this._fillObj = {
            colors: [
                { offset: 0, color: c0 },
                { offset: 1, color: c1 }
            ]
        };
        
        this._arrows = [];
        var rLength = this._arrowTotalLength();
        this._points = this._createArrowPoints();
        var _s = 0 - rLength;
        for(var i = 0; i <= this.getArrowCount(); i++) {
            var arrow = this.surface.createPolyline(this._points);
            // 5 points - 1,5 tail, 3 head, 2,4 head root 
                arrow.setFill(dojo.mixin({
                    type: "linear",
                    x1: this._points[0].x, y1: this._points[0].y,
                    x2: this._points[3].x, y2: this._points[0].y
                    }, this._fillObj));

            if(!this.reverse)
                arrow.setTransform({dx: _s});
            else 
                arrow.setTransform({xx: -1, dx: _s+rLength*2});
            this._arrows[i] = arrow;
            _s += rLength;
        }
    },
   
    _drawAxis: function(height){
    	
    	if(this.isValueStateOk()) {    		
            var sVal = hyperic.unit.UnitsConvert.convert(this.value, this.format, {pattern:this.getLabelFormat()});
            var fMax = hyperic.util.FontUtil.findGoodSizeFontByRect(sVal, this.width, height);
            if(this._text) {
                this._text.setShape({text: sVal});
            } else {
                this._text = this.drawText(sVal, this.width/2, this.height, "end", this.getLabelColor(), {family:"Helvetica",weight:"bold",size:fMax+'px'});
            }    
    	} else {
            if(this._text)
                this._text.setShape({text: ""});    		
    	}
    } 
    
    

});