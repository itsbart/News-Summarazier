/**
 * Created by jeffrey.spaulding on 11/29/2014.
 */

/*----------------------------------------------------------*/
// jslint options
/*----------------------------------------------------------*/
/*jslint browser: true, todo: true*/
/*global console, $*/

(function () {
    'use strict';
    var $resultsLoader = $("#results-loader"),
        $resultsList = $("#results-list");

    function processResults(results) {
        var i,
            docs,
            doc,
            entry,
            j;

        if (results.response && results.response.docs) {
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
        } else {
            $.bootstrapGrowl("Error processing query", {type: "danger", delay: 15000});
        }
    }

    $(document).ready(function () {
        $('#search-box').search();
        $('#search-box').on('searched.fu.search', function (event, queryStr) {
            /*jslint unparam:true*/
            $resultsList.empty();
            $resultsLoader.fadeIn();

            $.ajax({
                url: "query",
                data: {
                    query: queryStr
                },
                dataType: "json"
            }).done(function (results) {
                $resultsLoader.hide();
                processResults(results);

            }).fail(function (jqXHR, textStatus, errorThrown) {
                $.bootstrapGrowl("Error executing query", {type: "danger", delay: 15000});
            });
        });
        /*jslint unparam:false*/
    });
}());

