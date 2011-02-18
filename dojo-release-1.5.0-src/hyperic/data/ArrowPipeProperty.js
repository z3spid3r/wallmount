dojo.provide("hyperic.data.ArrowPipeProperty");

dojo.declare("hyperic.data.ArrowPipeProperty",null,{

    // arrowCount: Object
    //
    // value: how many arrows component is using
    // min: min constraint for value
    // max: max constraint for value

    // arrowGap: Object
    //
    // gap between arrows
    // min: min constraint for value
    // max: max constraint for value
    
    // arrowHeadLength: Object
    //
    // length of the arrow head
    // min: min constraint for value
    // max: max constraint for value

    constructor: function(){
        this.arrowCountObj = {value:3, min: 2, max: 6};
        this.arrowGapObj = {value:25, min: 5, max: 40};
        this.arrowHeadLengthObj = {value:15, min: 5, max: 40};
    },
    
    getArrowCount: function(){
        return this.arrowCountObj.value;
    },
    
    _setArrowCountAttr: function(count){
        this.arrowCountObj.value = count;
    },

    getArrowGap: function(){
        return this.arrowGapObj.value;
    },

    _setArrowGapAttr: function(gap){
        this.arrowGapObj.value = gap;
    },

    getArrowHeadLength: function(){
        return this.arrowHeadLengthObj.value;
    },
    
    _setArrowHeadLengthAttr: function(length){
        this.arrowHeadLengthObj.value = length;
    }
    
});