// Generated from contracts/openapi.yaml. Do not edit.
export const endpoints = {
  "register": {
    "path": "/api/v1/auth/register",
    "method": "POST",
    "public": true
  },
  "login": {
    "path": "/api/v1/auth/login",
    "method": "POST",
    "public": true
  },
  "refresh": {
    "path": "/api/v1/auth/refresh",
    "method": "POST",
    "public": false
  },
  "logout": {
    "path": "/api/v1/auth/logout",
    "method": "POST",
    "public": false
  },
  "forgotPassword": {
    "path": "/api/v1/auth/forgot-password",
    "method": "POST",
    "public": true
  },
  "resetPassword": {
    "path": "/api/v1/auth/reset-password",
    "method": "POST",
    "public": true
  },
  "getProfile": {
    "path": "/api/v1/users/me",
    "method": "GET",
    "public": false
  },
  "updateProfile": {
    "path": "/api/v1/users/me",
    "method": "PATCH",
    "public": false
  },
  "changePassword": {
    "path": "/api/v1/users/me/change-password",
    "method": "POST",
    "public": false
  },
  "listSessions": {
    "path": "/api/v1/users/me/sessions",
    "method": "GET",
    "public": false
  },
  "revokeSession": {
    "path": "/api/v1/users/me/sessions/{session_id}",
    "method": "DELETE",
    "public": false
  },
  "listDevices": {
    "path": "/api/v1/devices",
    "method": "GET",
    "public": false
  },
  "getDevice": {
    "path": "/api/v1/devices/{device_id}",
    "method": "GET",
    "public": false
  },
  "renameDevice": {
    "path": "/api/v1/devices/{device_id}",
    "method": "PATCH",
    "public": false
  },
  "bindDevice": {
    "path": "/api/v1/devices/bind",
    "method": "POST",
    "public": false
  },
  "unbindDevice": {
    "path": "/api/v1/devices/{device_id}/binding",
    "method": "DELETE",
    "public": false
  },
  "listBindings": {
    "path": "/api/v1/device-bindings",
    "method": "GET",
    "public": false
  },
  "getDeviceConfig": {
    "path": "/api/v1/devices/{device_id}/config",
    "method": "GET",
    "public": false
  },
  "updateDeviceConfig": {
    "path": "/api/v1/devices/{device_id}/config",
    "method": "PATCH",
    "public": false
  },
  "ackDeviceConfig": {
    "path": "/api/v1/devices/{device_id}/config/ack",
    "method": "POST",
    "public": false
  },
  "reportDeviceStatus": {
    "path": "/api/v1/devices/{device_id}/reports",
    "method": "POST",
    "public": false
  },
  "createFeedback": {
    "path": "/api/v1/feedbacks",
    "method": "POST",
    "public": false
  },
  "listFeedback": {
    "path": "/api/v1/feedbacks",
    "method": "GET",
    "public": false
  },
  "getFeedback": {
    "path": "/api/v1/feedbacks/{feedback_id}",
    "method": "GET",
    "public": false
  },
  "adminListUsers": {
    "path": "/api/v1/admin/users",
    "method": "GET",
    "public": false
  },
  "adminSetUserStatus": {
    "path": "/api/v1/admin/users/{user_id}/status",
    "method": "PATCH",
    "public": false
  },
  "adminCreateDevice": {
    "path": "/api/v1/admin/devices",
    "method": "POST",
    "public": false
  },
  "adminListDevices": {
    "path": "/api/v1/admin/devices",
    "method": "GET",
    "public": false
  },
  "adminSetDeviceStatus": {
    "path": "/api/v1/admin/devices/{device_id}/status",
    "method": "PATCH",
    "public": false
  },
  "adminListFeedback": {
    "path": "/api/v1/admin/feedbacks",
    "method": "GET",
    "public": false
  },
  "adminUpdateFeedback": {
    "path": "/api/v1/admin/feedbacks/{feedback_id}",
    "method": "PATCH",
    "public": false
  },
  "health": {
    "path": "/actuator/health",
    "method": "GET",
    "public": true
  },
  "liveness": {
    "path": "/actuator/health/liveness",
    "method": "GET",
    "public": true
  },
  "readiness": {
    "path": "/actuator/health/readiness",
    "method": "GET",
    "public": true
  }
} as const;
