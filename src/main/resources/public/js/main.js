/**
 * Created by jeffrey.spaulding on 11/29/2014.
 */

/*----------------------------------------------------------*/
// jslint options
/*----------------------------------------------------------*/
/*jslint browser: true, todo: true, unparam:true */
/*global console, $, moment, createStoryJS */

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
        $feedsLoader = $("#feeds-loader"),
        $activeTopic = null,
        ALL_TOPICS = "All Topics",
        LABEL_KEY = "label",
        curQueryStr = null,
        curResults = null,
        $floatingFeed = $("#floating-feed"),
        $addFeedButton = $("#add-feed-button"),
        $timelineView = $("#timeline-view"),
        isTimeLineShowing = false,
        timelineQueryStr,
        CUR_SEARCH = "Current Search",
        $activeFeed = null;

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
                if (typeof doc.title === "string") {
                    entry += doc.title;
                } else {
                    for (j = 0; j < doc.title.length; j = j + 1) {
                        entry += doc.title[j] + "&nbsp;";
                    }
                }
                entry += '</a><br><small>' + doc.url + '</small></h4><span>';
                if (doc.date) {
                    entry += '<em>' + moment(doc.date).format("MMM DD, YYYY") + "</em>&nbsp;-&nbsp;";
                }
                if (doc.content) {
                    if (typeof doc.content === "string") {
                        entry += doc.content.substr(0, 255);
                        entry += "...";
                    } else if (doc.content.length > 0) {
                        entry += doc.content[0];
                    }
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
            $activeTopic.data(LABEL_KEY, ALL_TOPICS);
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

    function initTimeline() {
        var timelineSource,
            timelineDocs = [],
            docs,
            i,
            doc,
            content,
            j;

        if (!curResults) {
            return;
        }

        if (timelineQueryStr !== curQueryStr) {
            timelineQueryStr = curQueryStr;

            docs = curResults.response.docs;

            for (i = 0; i < docs.length; i = i + 1) {
                doc = docs[i];

                content = "";
                for (j = 0; j < doc.content.length; j = j + 1) {
                    content += doc.content[j] + "<br>";
                }

                timelineDocs.push({
                    "startDate": moment(doc.date).format("YYYY,MM,DD"),
                    "headline": '<a href="' + doc.url + '" target="_blank">' + doc.title[0] + '</a>',
                    "text": content
                });
            }

            timelineSource = {
                "timeline": {
                    "headline": "Welcome to the Timeline for: " + curQueryStr,
                    "type": "default",
                    "text": "Click on the side arrows or use the Timeline slider below to navigate the news articles",
                    "date": timelineDocs
                }
            };

            createStoryJS({
                type: 'timeline',
                width: '100%',
                height: '100%',
                source: timelineSource,
                embed_id: 'timeline-view'
            });
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

        $timelineView.empty();
        if (isTimeLineShowing) {
            initTimeline();
        }

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

    function onFeedClick() {
        var label;

        if ($activeFeed) {
            $activeFeed.removeClass("active");
        }
        $activeFeed = $(this);
        $activeFeed.addClass("active");

        $resultsList.empty();
        label = $activeFeed.data(LABEL_KEY);
        if (label === CUR_SEARCH) {
            createResultsList(curResults.response.docs);
        } else if (label) {
            $resultsLoader.show();
            $.ajax({
                url: "feeds/" + label,
                dataType: "json"
            }).done(function (results) {
                $resultsLoader.hide();
                createResultsList(results.response.docs);
            }).fail(function (jqXHR, textStatus, errorThrown) {
                $.bootstrapGrowl("Error executing query/cluster", {type: "danger", delay: 15000});
            });
        }

    }

    function checkFeeds() {
        $feedsLoader.show();

        $.ajax({
            url: "feeds",
            dataType: "json"
        }).done(function (results) {
            var i,
                entry;

            $feedsLoader.hide();

            if (results && results.length > 0) {
                $feedsHint.hide();
                $feedsList.empty();

                entry = '<a href="#" class="list-group-item active">' + CUR_SEARCH + '</a>';
                $activeFeed = $(entry);
                $activeFeed.data(LABEL_KEY, CUR_SEARCH);
                $activeFeed.click(onFeedClick);
                $feedsList.append($activeFeed);

                for (i = 0; i < results.length; i = i + 1) {
                    entry = '<a href="#" class="list-group-item">' + results[i] + '</a>';

                    // feeds click handler
                    entry = $(entry);
                    entry.click(onFeedClick);

                    entry.data(LABEL_KEY, results[i]);

                    $feedsList.append(entry);
                }
            } else {
                $feedsHint.show();
            }

        }).fail(function (jqXHR, textStatus, errorThrown) {
            $.bootstrapGrowl("Error fetching feeds", {type: "danger", delay: 15000});
        });
    }

    function addFeed() {
        if (curQueryStr) {
            $feedsHint.hide();
            $feedsLoader.show();
            $.ajax({
                url: "feeds/" + curQueryStr + "/create",
                dataType: "text"
            }).done(function (results) {
                checkFeeds();
            }).fail(function (jqXHR, textStatus, errorThrown) {
                $.bootstrapGrowl("Error creating feed", {type: "danger", delay: 15000});
            });
        }
    }

    $(document).ready(function () {
        var $introSearchBox = $("#intro-search-box"),
            $searchBox = $("#search-box");

        $(window).resize(onResize);
        setTimeout(function () {
            $introSearchBox.find("input").focus();
        }, 500);

        $introSearchBox.search();
        $introSearchBox.on('searched.fu.search', function (event, queryStr) {
            $("#background").fadeOut();
            $("#intro-search").remove();
            $("#main-navbar, #main-tabs, #viewport").removeClass("hidden");
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

        $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
            if ($(e.target).attr("href") === "#timeline") {
                isTimeLineShowing = true;
                initTimeline();
            } else {
                onResize();
                isTimeLineShowing = false;
            }
        });

        checkFeeds();
    });
}());

