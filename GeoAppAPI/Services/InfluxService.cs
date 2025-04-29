using GeoAppAPI.Models;
using InfluxDB.Client;
using InfluxDB.Client.Api.Domain;
using InfluxDB.Client.Linq;

namespace GeoAppAPI.Services;

public class InfluxService(InfluxDBClient influxDbClient)
{
    public async Task<List<Data>> QueryAsync(string userId, 
        DateTimeOffset? from, 
        DateTimeOffset? to, 
        CancellationToken cancellationToken)
    {
        var bucketName = _getBucketName(userId);
        await _ensureBucketExistsAsync(bucketName, cancellationToken).ConfigureAwait(false);
        
        var api = influxDbClient.GetQueryApi();

        var query =  InfluxDBQueryable<Data>
            .Queryable(bucket: bucketName, Organisation, api)
            .GetAsyncEnumerator(cancellationToken);

        if (from.HasValue)
        {
            query = query.Where(x => x.Timestamp >= from.Value);
        }

        if (to.HasValue)
        {
            query = query.Where(x => x.Timestamp <= to.Value);
        }
        
        var results = await query
            .ToListAsync(cancellationToken);
        
        return results;
    }

    public async Task WriteAsync(string userId, IEnumerable<Data> data, CancellationToken cancellationToken)
    {
        var bucketName = _getBucketName(userId);
        await _ensureBucketExistsAsync(bucketName, cancellationToken).ConfigureAwait(false);
        
        var writeApi = influxDbClient.GetWriteApiAsync();

        await writeApi
            .WriteMeasurementsAsync<Data>(data.ToList(), bucket: bucketName, org: Organisation, cancellationToken: cancellationToken)
            .ConfigureAwait(false);
    }
    
    private async Task _ensureBucketExistsAsync(string bucketName, CancellationToken cancellationToken)
    {
        var orgId = await _ensureOrganisationExistsAsync(cancellationToken).ConfigureAwait(false);
        
        var bucketsApi = influxDbClient.GetBucketsApi();

        var bucket = await bucketsApi
            .FindBucketByNameAsync(bucketName, cancellationToken)
            .ConfigureAwait(false);

        if (bucket == null)
        {
            var retentionRule = new BucketRetentionRules(everySeconds: 0);
            await bucketsApi
                .CreateBucketAsync(bucketName, retentionRule, orgId, cancellationToken)
                .ConfigureAwait(false);
        }
    }
    
    private async Task<string> _ensureOrganisationExistsAsync(CancellationToken cancellationToken)
    {
        var organisationsApi = influxDbClient.GetOrganizationsApi();
        
        var organisations = await organisationsApi
            .FindOrganizationsAsync(cancellationToken: cancellationToken)
            .ConfigureAwait(false);

        var organisation = organisations
            .FirstOrDefault(org => org.Name == Organisation); 
            
        organisation ??= await organisationsApi
            .CreateOrganizationAsync(Organisation, cancellationToken)
            .ConfigureAwait(false);

        return organisation.Id;
    }

    private static string _getBucketName(string userId)
    {
        return $"user-{userId}";
    }
    
    private const string Organisation = "GeoApp";
}