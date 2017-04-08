(function () {
    'use strict';

    angular.module('app.ui')
        .directive('uiTime', uiTime)
        .directive('uiNotCloseOnClick', uiNotCloseOnClick)
        .directive('slimScroll', slimScroll)
        .directive('imgHolder', imgHolder);

    function uiTime() {
        var directive = {
            restrict: 'A',
            link: link
        };

        return directive;

        function link(scope, ele) {
            var checkTime, startTime;

            startTime = function() {
                var h, m, s, t, time, today;
                today = new Date();
                h = today.getHours();
                m = today.getMinutes();
                s = today.getSeconds();
                m = checkTime(m);
                s = checkTime(s);
                time = h + ":" + m + ":" + s;
                ele.html(time);
                return t = setTimeout(startTime, 500);
            };

            checkTime = function(i) {
                if (i < 10) {
                    i = "0" + i;
                }
                return i;
            };

            startTime();
        }  
    }

    function uiNotCloseOnClick() {
        return {
            restrict: 'A',
            compile: function(ele, attrs) {
                return ele.on('click', function(event) {
                    return event.stopPropagation();
                });
            }
        };
    }

    function slimScroll() {
        return {
            restrict: 'A',
            link: function(scope, ele, attrs) {
                return ele.slimScroll({
                    height: attrs.scrollHeight || '100%'
                });
            }
        };
    }

    function imgHolder() {
        return {
            restrict: 'A',
            link: function(scope, ele, attrs) {
                return Holder.run({
                    images: ele[0]
                });
            }
        };
    }

})(); 