/**
 * Created by jeffrey.spaulding on 11/29/2014.
 */

/*----------------------------------------------------------*/
// jslint options
/*----------------------------------------------------------*/
/*jslint browser: true, todo: true, unparam:true */
/*global console, $*/

(function () {
    'use strict';
    var $resultsLoader = $("#results-loader"),
        $resultsList = $("#results-list"),
        $topicsPanel = $("#topics-panel"),
        $topicsLoader = $("#topics-loader"),
        $topicsList = $("#topics-list"),
        $activeTopic = null;

    function onResize() {
        var h = $topicsPanel.height();
        h -= $topicsPanel.find(".panel-heading").outerHeight();
        h -= 15;
        $topicsPanel.find(".panel-body").height(h);
    }

    function onTopicClick() {
        if ($activeTopic) {
            $activeTopic.removeClass("active");
        }
        $activeTopic = $(this);
        $activeTopic.addClass("active");
    }

    function processResults(results) {
        var i,
            docs,
            doc,
            entry,
            j,
            clusters,
            cluster;

        if (results.response && results.response.docs) {

            // Process Docs
            if (results.response.docs) {
                docs = results.response.docs;
                for (i = 0; i < docs.length; i = i + 1) {
                    doc = docs[i];

                    // Build the list entry
                    entry = '<div><h4><a href="' + doc.url + '" target="_blank">';
                    for (j = 0; j < doc.title.length; j = j + 1) {
                        entry += doc.title[j] + "&nbsp;";
                    }
                    entry += '</a><br><small>' + doc.url + '</small></h4><span>';
                    if (doc.date) {
                        entry += doc.date + "&nbsp;-&nbsp;";
                    }
                    if (doc.content && doc.content.length > 0) {
                        entry += doc.content[0];
                    }
                    entry += '</span></div>';
                    $resultsList.append(entry);
                }
            }

            // Process Topics/Clusters
            if (results.clusters) {
                clusters = results.clusters;
                // Build the cluster entries
                entry = '<a href="#" class="list-group-item active">All Topics (';
                entry += docs.length + ')</a>';
                $activeTopic = $(entry);
                $activeTopic.click(onTopicClick);
                $topicsList.append($activeTopic);

                for (i = 0; i < clusters.length; i = i + 1) {
                    cluster = clusters[i];
                    entry = '<a href="#" class="list-group-item subtopic">';
                    for (j = 0; j < cluster.labels.length; j = j + 1) {
                        if (j !== 0) {
                            entry += ", ";
                        }
                        entry += cluster.labels[j];
                    }
                    if (cluster.docs) {
                        entry += " (" + cluster.docs.length + ")";
                    }
                    entry += '</a>';

                    // topic click handler
                    entry = $(entry);
                    entry.click(onTopicClick);
                    $topicsList.append(entry);
                }
            }
        } else {
            $.bootstrapGrowl("Error processing query", {type: "danger", delay: 15000});
        }
    }

    function onSearch(event, queryStr) {

        $topicsList.hide();
        $topicsList.empty();
        $topicsLoader.fadeIn();

        $resultsList.empty();
        $resultsLoader.fadeIn();

        $.ajax({
            url: "query",
            data: {
                query: queryStr
            },
            dataType: "json"
        }).done(function (results) {
            $topicsLoader.hide();
            $topicsList.show();
            $resultsLoader.hide();
            processResults(results);

        }).fail(function (jqXHR, textStatus, errorThrown) {
            $.bootstrapGrowl("Error executing query", {type: "danger", delay: 15000});
        });
    }

    $(document).ready(function () {
        var $introSearchBox = $("#intro-search-box"),
            $searchBox = $("#search-box");

        $(window).resize(onResize);

        $introSearchBox.search();
        $introSearchBox.on('searched.fu.search', function (event, queryStr) {
            $("#background").fadeOut();
            $("#intro-search").remove();
            $("#main-navbar, #viewport").removeClass("hidden");
            $("#search-box input").val(queryStr);
            $("#search-box button").click();
            onResize();
        });

        $searchBox.search();
        $searchBox.on('searched.fu.search', onSearch);
    });
}());

