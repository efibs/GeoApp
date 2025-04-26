using System.Text.Json;
using GeoAppAPI.Models;
using Microsoft.AspNetCore.Authorization;

namespace GeoAppAPI.Auth;

public class PermissionRequirementHandler : AuthorizationHandler<PermissionRequirement>
{
    protected override Task HandleRequirementAsync(AuthorizationHandlerContext context, PermissionRequirement requirement)
    {
        var requirements = context.Requirements;
        
        // Find the permissions
        var permissionsString = context.User.FindFirst(c => c.Type == Permissions.PermissionClaimType)?.Value;
        
        // If the token doesn't have the permissions claim
        if (string.IsNullOrWhiteSpace(permissionsString))
        {
            context.Fail(new AuthorizationFailureReason(this, "User token has no permissions"));
            return Task.CompletedTask;
        }

        // Parse to list of strings
        var userPermissions = JsonSerializer.Deserialize<string[]>(permissionsString);

        // If the token doesn't have the permissions claim
        if (userPermissions == null)
        {
            context.Fail(new AuthorizationFailureReason(this, "Permissions in user token could not be parsed"));
            return Task.CompletedTask;
        }
        
        // If the token has the all permissions claim
        if (userPermissions.Any(p => p == Permissions.All))
        {
            context.Succeed(requirement);
            return Task.CompletedTask;
        }
        
        // Get the required permissions
        var requiredPermissions = requirements
            .Where(r => r.GetType() == typeof(PermissionRequirement))
            .Select(r => ((PermissionRequirement)r).Permission)
            .ToArray();
        
        // Check if there is a required role missing
        var permissionMissing = requiredPermissions
            .Any(p => userPermissions
                .Any(up => up == p) == false);

        if (permissionMissing)
        {
            context.Fail(new AuthorizationFailureReason(this, "User token does not grant the necessary permissions"));
            return Task.CompletedTask;
        }
        
        context.Succeed(requirement);
        return Task.CompletedTask;
    }
}