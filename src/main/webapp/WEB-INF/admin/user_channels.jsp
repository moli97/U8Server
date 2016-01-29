<%--
  Created by IntelliJ IDEA.
  User: xiaohei
  Date: 2015/8/29
  Time: 11:48
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String path = request.getContextPath();
    String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path;

%>
<base href="<%=basePath%>">
<html>
<head>
    <title></title>

    <link rel="stylesheet" type="text/css" href="<%=basePath%>/js/plugins/easyui/themes/default/easyui.css">
    <link rel="stylesheet" type="text/css" href="<%=basePath%>/js/plugins/easyui/themes/icon.css">
    <link rel="stylesheet" type="text/css" href="<%=basePath%>/js/plugins/easyui/themes/color.css">

    <script type="text/javascript" src="<%=basePath%>/js/plugins/easyui/jquery.min.js"></script>
    <script type="text/javascript" src="<%=basePath%>/js/plugins/easyui/jquery.easyui.min.js"></script>
    <script type="text/javascript" src="<%=basePath%>/js/plugins/easyui/locale/easyui-lang-zh_CN.js"></script>

    <script src="http://cdn.hcharts.cn/highcharts/highcharts.js" type="text/javascript"></script>
    <script src="http://cdn.hcharts.cn/highcharts/modules/exporting.js" type="text/javascript"></script>

</head>
<body>
    <div>
        <form id="fm" method="post" novalidate>
            <input id="games" type="text" class="easyui-combobox" name="allgames" maxlength="255" required="true"/>
        </form>
    </div>

    <div id="container" style="min-width:700px;height:400px"></div>

<script type="text/javascript">



$(function () {
    $("#games").combobox({
        url:'<%=basePath%>/admin/games/getAllGamesSimple',
        valueField:'appID',
        textField:'name',
        onLoadSuccess:function(){
            var val = $(this).combobox("getData");
            for(var item in val[0]){
                if(item == 'appID'){
                    $(this).combobox("select", val[0][item]);
                }
            }
        },
        onSelect:function(rec){

            $.post('<%=basePath%>/admin/users/getUserChannelAnalytics', {appID:rec.appID}, function(result){

                var cc = "[{name: 'Microsoft Internet Explorer', y: 56.33}, {name: 'Chrome', y: 24.03, sliced: true, selected: true}, {name: 'Firefox', y: 10.38}, {name: 'Safari', y: 4.77}, {name: 'Opera', y: 0.91}, {name: 'Proprietary or Undetectable', y: 0.2}]";
                alert(cc);
                $('#container').highcharts({ chart: { plotBackgroundColor: null, plotBorderWidth: null, plotShadow: false }, title: { text: 'Browser market shares at a specific website, 2010' }, tooltip: { pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>' }, plotOptions: { pie: { allowPointSelect: true, cursor: 'pointer', dataLabels: { enabled: true, color: '#000000', connectorColor: '#000000', format: '<b>{point.name}</b>: {point.percentage:.1f} %' } } }, series: [{ type: 'pie', name: 'Browser share', data: cc }] });

            }, 'text');

        }
    });
});
</script>

</body>



</html>
