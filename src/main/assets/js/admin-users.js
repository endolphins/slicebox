(function () {
   'use strict';
}());

angular.module('slicebox.adminUsers', ['ngRoute'])

.config(function($routeProvider) {
  $routeProvider.when('/admin/users', {
    templateUrl: '/assets/partials/adminUsers.html',
    controller: 'AdminUsersCtrl'
  });
})

.controller('AdminUsersCtrl', function($scope, $http) {
    // Initialization
    $scope.objectActions =
        [
            {
                name: 'Delete',
                action: $scope.confirmDeleteEntitiesFunction('/api/users/', 'user(s)')
            }
        ];

    $scope.callbacks = {};

    // Scope functions
    $scope.loadUsersPage = function(startIndex, count, orderByProperty, orderByDirection) {
        return $http.get('/api/users');
    };

    $scope.addUserButtonClicked = function() {
        $scope.addEntityButtonClicked('addUserModalContent.html', 'AddUserModalCtrl', '/api/users', 'User', $scope.callbacks.usersTable);
    };
})

.controller('AddUserModalCtrl', function($scope, $mdDialog) {
    // Initialization
    $scope.role = 'USER';

    // Scope functions
    $scope.addButtonClicked = function() {
        $mdDialog.hide({ user: $scope.userName, password: $scope.password, role: $scope.role});
    };

    $scope.cancelButtonClicked = function() {
        $mdDialog.cancel();
    };
});