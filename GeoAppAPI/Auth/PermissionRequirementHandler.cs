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
        var userPermissions = context.User
            .FindAll(c => c.Type == Permissions.PermissionClaimType)
            .Select(c => c.Value)
            .ToList();
        
        // If the token doesn't have the permissions claim
        if (userPermissions.Count == 0)
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