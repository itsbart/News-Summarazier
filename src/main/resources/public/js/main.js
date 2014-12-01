/**
 * Created by jeffrey.spaulding on 11/29/2014.
 */

/*----------------------------------------------------------*/
// jslint options
/*----------------------------------------------------------*/
/*jslint browser: true, todo: true, unparam:true */
/*global console, $, moment */

(function () {
    'use strict';
    var $resultsLoader = $("#results-loader"),
        $resultsList = $("#results-list"),
        $topicsPanel = $("#topics-panel"),
        $topicsLoader = $("#topics-loader"),
        $topicsList = $("#topics-list"),
        $feedsPanel = $("#feeds-panel"),
        $feedsList = $("#feeds-list"),
        $feedsHint = $("#feeds-hint"),
        $activeTopic = null,
        ALL_TOPICS = "All Topics",
        LABEL_KEY = "label",
        curQueryStr = null,
        curResults = null,
        $floatingFeed = $("#floating-feed"),
        $addFeedButton = $("#add-feed-button");

    function onResize() {
        var h = $topicsPanel.height();
        h -= $topicsPanel.find(".panel-heading").outerHeight();
        h -= 15;
        $topicsPanel.find(".panel-body").height(h);

        h = $feedsPanel.height();
        h -= 15;
        $feedsPanel.find(".panel-body").height(h);
    }

    function createResultsList(docs) {
        var i,
            doc,
            entry,
            j;

        if (docs) {
            for (i = 0; i < docs.length; i = i + 1) {
                doc = docs[i];

                // Build the list entry
                entry = '<div><h4><a href="' + doc.url + '" target="_blank">';
                for (j = 0; j < doc.title.length; j = j + 1) {
                    entry += doc.title[j] + "&nbsp;";
                }
                entry += '</a><br><small>' + doc.url + '</small></h4><span>';
                if (doc.date) {
                    entry += '<em>' + moment(doc.date).format("MMM DD, YYYY") + "</em>&nbsp;-&nbsp;";
                }
                if (doc.content && doc.content.length > 0) {
                    entry += doc.content[0];
                }
                entry += '</span></div>';
                $resultsList.append(entry);
            }
        } else {
            $.bootstrapGrowl("Error creating results list", {type: "danger", delay: 15000});
        }
    }

    function onTopicClick() {
        var label;

        if ($activeTopic) {
            $activeTopic.removeClass("active");
        }
        $activeTopic = $(this);
        $activeTopic.addClass("active");

        $resultsList.empty();
        label = $activeTopic.data(LABEL_KEY);
        if (label === ALL_TOPICS) {
            createResultsList(curResults.response.docs);
        } else if (label) {
            $resultsLoader.show();
            $.ajax({
                url: "query/cluster",
                data: {
                    query: curQueryStr,
                    cluster: label
                },
                dataType: "json"
            }).done(function (docs) {
                $resultsLoader.hide();
                createResultsList(docs);
            }).fail(function (jqXHR, textStatus, errorThrown) {
                $.bootstrapGrowl("Error executing query/cluster", {type: "danger", delay: 15000});
            });
        }
    }

    function createTopicsList(numDocs, clusters) {
        var i,
            entry,
            cluster,
            label;

        if (clusters) {
            // Process Topics/Clusters

            // Build the cluster entries
            entry = '<a href="#" class="list-group-item active">' + ALL_TOPICS + ' (';
            entry += numDocs + ')</a>';
            $activeTopic = $(entry);
            $activeTopic.click(onTopicClick);
            $activeTopic.data("label", ALL_TOPICS);
            $topicsList.append($activeTopic);

            for (i = 0; i < clusters.length; i = i + 1) {
                cluster = clusters[i];
                entry = '<a href="#" class="list-group-item subtopic">';
                if (cluster.labels && cluster.labels.length > 0) {
                    label = cluster.labels[0];
                    entry += label;
                }
                if (cluster.docs) {
                    entry += " (" + cluster.docs.length + ")";
                }
                entry += '</a>';

                // topic click handler
                entry = $(entry);
                entry.click(onTopicClick);

                // Save label data for topic click handler
                entry.data(LABEL_KEY, label);

                $topicsList.append(entry);
            }
        } else {
            $.bootstrapGrowl("Error processing clusters", {type: "danger", delay: 15000});
        }
    }

    function processResults(results) {
        if (results.response && results.response.docs) {
            createResultsList(results.response.docs);
            createTopicsList(results.response.docs.length, results.clusters);
        } else {
            $.bootstrapGrowl("Error processing query", {type: "danger", delay: 15000});
        }
    }


    function onSearch(event, queryStr) {
        curQueryStr = queryStr;
        $topicsList.hide();
        $topicsList.empty();
        $topicsLoader.fadeIn();

        $resultsList.empty();
        $resultsLoader.fadeIn();
        $addFeedButton.removeAttr("disabled");

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
            curResults = results;
            processResults(results);

        }).fail(function (jqXHR, textStatus, errorThrown) {
            $.bootstrapGrowl("Error executing query", {type: "danger", delay: 15000});
        });
    }

    function addFeed() {
        var entry;
        if (curQueryStr) {
            $feedsHint.hide();
            entry = '<a href="#" class="list-group-item">' + curQueryStr + '</a>';
            $feedsList.append(entry);
        }
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

        $addFeedButton.click(function () {
            var pos = $addFeedButton.position();
            $floatingFeed.text(curQueryStr);
            $floatingFeed.css({
                top: 9,
                left: 193
            });
            $floatingFeed.show();
            pos.top += 100;
            pos.left -= 40;
            $floatingFeed.animate(pos, "slow", function () {
                $floatingFeed.fadeOut();
                addFeed();
                $addFeedButton.attr("disabled", "disabled");
            });
        });
    });
}());

