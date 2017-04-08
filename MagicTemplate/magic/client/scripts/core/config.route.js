(function () {
    'use strict';

    angular.module('app')
        .config(['$routeProvider', function($routeProvider) {
            var routes, setRoutes;

            routes = [
                'dashboard',
                'ui/typography', 'ui/buttons', 'ui/icons', 'ui/grids', 'ui/widgets', 'ui/components', 'ui/boxes', 'ui/timeline', 'ui/nested-lists', 'ui/pricing-tables', 'ui/maps',
                'tables/static', 'tables/dynamic', 'tables/responsive',
                'forms/elements', 'forms/layouts', 'forms/validation', 'forms/wizard',
                'charts/charts', 'charts/flot',
                'pages/404', 'pages/500', 'pages/blank', 'pages/forgot-password', 'pages/invoice', 'pages/lock-screen', 'pages/profile', 'pages/invoice', 'pages/signin', 'pages/signup',
                'mail/compose', 'mail/inbox', 'mail/single',
                'app/tasks', 'app/calendar'
            ]

            setRoutes = function(route) {
                var config, url;
                url = '/' + route;
                config = {
                    templateUrl: 'views/' + route + '.html'
                };
                $routeProvider.when(url, config);
                return $routeProvider;
            };

            routes.forEach(function(route) {
                return setRoutes(route);
            });

            $routeProvider
                .when('/', {redirectTo: '/dashboard'})
                .when('/404', {templateUrl: 'views/pages/404.html'})
                .otherwise({ redirectTo: '/404'});

        }]
    );

})(); 