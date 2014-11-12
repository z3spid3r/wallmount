<html>

  <head>
    <meta http-equiv="content-script-type" content="text/javascript">
    <title>Wallmount Player - ${useLayout}</title>

    <link rel="stylesheet" type="text/css" title="Basic"
          href="../public/js/hyperic/wallmount/resources/PlayerCombined.css" />

    <link rel="alternate stylesheet" type="text/css" title="Night"
          href="../public/js/hyperic/wallmount/resources/PlayerCombinedNight.css" />

    <link rel="alternate stylesheet" type="text/css" title="Matrix"
          href="../public/js/hyperic/wallmount/resources/PlayerCombinedMatrix.css" />
      
    <script type="text/javascript"
            src="../public/js/dojo/dojo.js"
            djConfig="parseOnLoad: true, isDebug: false, locale: 'pt-pt', extraLocale: ['en-us']"
            gfxRenderer: 'canvas'></script>
          
    <script type="text/javascript">
      
      dojo.addOnLoad(function(){
        hyperic.wallmount.base.metricStore = new hyperic.data.MetricStore(
            {url: "/hqu/wmvisualizer/metricstore/getMetrics.hqu",
             idToBaseUrl: false}
        );
        
        hyperic.wallmount.base.metricStore.sync(true);
        
        <% if (lparam == 'layout') { %> 
        var url = "/hqu/wmvisualizer/wmvisualizer/getLayout.hqu?layout=${useLayout}";
        <% } else { %>
        var url = "/hqu/wmvisualizer/dyntemplate/executeDynlayout.hqu?dynlayout=${useLayout}";
        <% } %>
        hyperic.wallmount.Player.loadLayout(url,true);
      }); 
      
    </script>
        
  </head>

  
  <body class="claro">
    <div dojoType="hyperic.wallmount.Registry"
         id="registry"
         jsId="hyperic.wallmount.Registry._registry"
         plugins="[
             {plugin: 'hyperic.widget.EllipseLabel', internal:{bgImageURI:'/hqu/wmvisualizer/image/getScaledImage.hqu?path=js/hyperic/widget/label/resources/ellipse-green.png&w=<%= '${bgImageWidth}' %>&h=<%= '${bgImageHeight}' %>'}},
             {plugin: 'hyperic.widget.AvailText', internal:{bgImageURI:'/hqu/wmvisualizer/image/getScaledImage.hqu?path=js/hyperic/widget/avail/resources/<%= '${bgImageStatus}' %>-ellipse.png&w=<%= '${bgImageWidth}' %>&h=<%= '${bgImageHeight}' %>'}},
             {plugin: 'hyperic.widget.AvailIcon', internal:{bgImageURI:'/hqu/wmvisualizer/image/getScaledImage.hqu?path=js/hyperic/widget/avail/resources/<%= '${bgImageStatus}' %>-<%= '${bgImageRes}' %>.png&w=<%= '${bgImageWidth}' %>&h=<%= '${bgImageHeight}' %>'}}
         ]"></div>  
    <div id="wallmountpane"></div>
  </body>

</html>