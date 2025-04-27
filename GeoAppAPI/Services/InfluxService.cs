using GeoAppAPI.Models;
using InfluxDB.Client;
using InfluxDB.Client.Api.Domain;
using InfluxDB.Client.Linq;

namespace GeoAppAPI.Services;

public class InfluxService(InfluxDBClient influxDbClient)
{
    public async Task<List<Data>> QueryAsync(string userId)
    {
        var bucketName = _getBucketName(userId);
        await _ensureBucketExistsAsync(bucketName).ConfigureAwait(false);
        
        var api = influxDbClient.GetQueryApi();
        
        var results = await InfluxDBQueryable<Data>
            .Queryable(bucket: bucketName, Organisation, api)
            .GetAsyncEnumerator()
            .ToListAsync()
            .ConfigureAwait(false);
        
        return results;
    }

    public async Task WriteAsync(string userId, IEnumerable<Data> data)
    {
        var bucketName = _getBucketName(userId);
        await _ensureBucketExistsAsync(bucketName).ConfigureAwait(false);
        
        var writeApi = influxDbClient.GetWriteApiAsync();

        await writeApi
            .WriteMeasurementsAsync<Data>(data.ToList(), bucket: bucketName, org: Organisation)
            .ConfigureAwait(false);
    }
    
    private async Task _ensureBucketExistsAsync(string bucketName)
    {
        var orgId = await _ensureOrganisationExistsAsync().ConfigureAwait(false);
        
        var bucketsApi = influxDbClient.GetBucketsApi();

        var bucket = await bucketsApi
            .FindBucketByNameAsync(bucketName)
            .ConfigureAwait(false);

        if (bucket == null)
        {
            var retentionRule = new BucketRetentionRules(everySeconds: 0);
            await bucketsApi
                .CreateBucketAsync(bucketName, retentionRule, orgId)
                .ConfigureAwait(false);
        }
    }
    
    private async Task<string> _ensureOrganisationExistsAsync()
    {
        var organisationsApi = influxDbClient.GetOrganizationsApi();
        
        var organisations = await organisationsApi
            .FindOrganizationsAsync()
            .ConfigureAwait(false);

        var organisation = organisations
            .FirstOrDefault(org => org.Name == Organisation); 
            
        organisation ??= await organisationsApi
            .CreateOrganizationAsync(Organisation)
            .ConfigureAwait(false);

        return organisation.Id;
    }

    private static string _getBucketName(string userId)
    {
        return $"user-{userId}";
    }
    
    private const string Organisation = "GeoApp";
}