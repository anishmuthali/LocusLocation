(function () {
    'use strict';

    angular.module('app.ui.form.validation')
        .directive('validateEquals', validateEquals);

    // used for confirm password
    // Note: if you modify the "confirm" input box, and then update the target input box to match it, it'll still show invalid style though the values are the same now
    // Note2: also remember to use " ng-trim='false' " to disable the trim
    function validateEquals() {
        var directive = {
            require: 'ngModel',
            link: link
        };

        return directive;

        function link(scope, ele, attrs, ngModelCtrl) {
            var validateEqual;

            validateEqual = function(value) {
                var valid;
                valid = value === scope.$eval(attrs.validateEquals);
                ngModelCtrl.$setValidity('equal', valid);
                typeof valid === "function" ? valid({
                    value: void 0
                }) : void 0;
            };

            ngModelCtrl.$parsers.push(validateEqual);

            ngModelCtrl.$formatters.push(validateEqual);

            scope.$watch(attrs.validateEquals, function(newValue, oldValue) {
                if (newValue !== oldValue) {
                    ngModelCtrl.$setViewValue(ngModelCtrl.$ViewValue);
                }
            });

        }
    }

})(); 