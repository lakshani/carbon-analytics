<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<!--
~ Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<fmt:bundle basename="org.wso2.carbon.analytics.spark.ui.i18n.Resources">
    <carbon:breadcrumb label="spark.conosle.menu"
                       resourceBundle="org.wso2.carbon.analytics.spark.ui.i18n.Resources"
                       topPage="true" request="<%=request%>"/>

    <link type="text/css" href="css/main.css" rel="stylesheet"/>

    <script src="js/jquery-1.7.1.min.js"></script>
    <script src="js/jquery.mousewheel-min.js"></script>
    <script src="js/jquery.terminal-min.js"></script>
    <link href="css/jquery.terminal.css" rel="stylesheet"/>

    <%--<link type="text/css" href="css/ui.all.css" rel="stylesheet"/>--%>
    <%--<script type="text/javascript" src="js/jquery-ui-1.6.custom.min.js"></script>--%>
    <%--<script type="text/javascript" src="js/jquery.hoverIntent.js"></script>--%>
    <%--<script type="text/javascript" src="js/jquery.cluetip.js"></script>--%>

    <div id="middle">
        <h2>Interactive Spark Console</h2>

        <div id="workArea">

            <div id="terminalArea">

                <script>
                    $(document).ready(function ($) {
                        var id = 1;
                        $('#terminalArea').terminal(function (command, term) {
                            $('#terminalArea').addClass('terminalArea');
                            if ($.trim(command) == "") {
                                term.resume();
                            }
                            else if ($.trim(command) == 'help') {
                                term.echo("Please refer documentation!");
                            } else {
                                term.pause();
                                jQuery.ajax({
                                                url: '../spark-console/execute_spark_ajaxprocessor.jsp',
                                                type: 'POST',
                                                data: {
                                                    'query': command
                                                },
                                                dataType: 'json',
                                                success: function (data) {
                                                    // alert('Data: '+data.response.items.length);
                                                    console.log(data);
                                                    term.resume();
                                                    term.echo(data.meta.responseMessage);

                                                    var results = data.response.items;
                                                    if (results.length != 0) {
                                                        term.echo(results.length + " results returned!");
                                                        var columns = data.meta.columns;
                                                        var columnRow = "";
                                                        for (var i = 0; i < columns.length; i++) {
                                                            columnRow = columnRow + columns[i] + "\t|\t";
                                                        }
                                                        term.echo(columnRow);

                                                        var row = "";
                                                        for (var i = 0; i < results.length; i++) {
                                                            var element = results[i];
                                                            row = "";
                                                            for (var j = 0; j < columns.length; j++) {
                                                                row = row + element[j] + "\t"
                                                            }
                                                            ;
                                                            term.echo(row);
                                                        }
                                                    }
                                                },
                                                error: function (xhr, error, errorThrown) {
                                                    console.log($(xhr.responseText)[3].innerHTML);
                                                    term.error('ERROR : ' + $(xhr.responseText)[3].innerHTML);
                                                    term.resume();
                                                }
                                            }); //end of ajax

                            }
                            ;
                        }, {
                                                        greetings: "Welcome to interactive Spark SQL shell\n" +
                                                                   "This interactive shell lets you execute Spark SQL commands against a local Spark cluster \n" +
                                                                   "Initializing Spark client...",
                                                        prompt: "SparkSQL>",
                                                        onBlur: function () {
                                                            // prevent loosing focus
                                                            return false;
                                                        }
                                                    });
                    })
                    ;

                </script>
            </div>

        </div>
    </div>

</fmt:bundle>
